package com.atd.microservices.core.ediwriter.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

import com.atd.microservices.core.ediwriter.utils.EDIValidatorService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.atd.microservices.core.ediwriter.domain.EDIData;
import com.atd.microservices.core.ediwriter.domain.EDIWriterPayload;
import com.atd.microservices.core.ediwriter.exception.EDIWriterException;
import com.atd.microservices.core.ediwriter.webclients.EDIAnalyticsDataClient;
import com.atd.microservices.core.ediwriter.webclients.EDIConfigClient;
import com.berryworks.edireader.json.toedi.JsonToEdi;
import com.berryworks.ediwriter.envelope.EnvelopeSpecification;
import com.berryworks.ediwriter.envelope.EnvelopeSpecificationImpl;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class EDIProcessor {

	@Autowired
	private EDIAnalyticsDataClient ediAnalyticsDataClient;

	@Autowired
	private EDIReaderUtil ediReaderUtil;

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	private EDIJsonHeaderUtil ediJsonHeaderUtil;

	@Value("${ediwriter.fusePath}")
	private String fusePath;

	@Autowired
	private EDIWriterMetrics ediWriterMetrics;
	
	@Autowired
	private EDIGrouperService ediGrouperService;
	
	@Autowired
	private EDIConfigClient ediConfigClient;

	@Autowired
	private EDIValidatorService ediValidatorService;

	public String process(EDIWriterPayload ediWriterPayload) {

		String ediString = null;
		String documentType = null;
		Map<String, String> headerValues = null;
		try {
			JsonNode ediJson = ediWriterPayload.getData();
			log.debug("ediData: {}", ediJson);
			headerValues = ediJsonHeaderUtil.extractHeaderInfoFromEDIDoc(ediJson);
			documentType = headerValues.get("type");
			// Convert the EDI Json to raw EDI document
			ediString = convertJsonToEDI(ediWriterPayload);
			log.debug("EDI raw string: {}", ediString);

			String acknowledgement = null;
			try {
				acknowledgement = ediValidatorService.validate(ediString);
			} catch (Exception e) {
				throw new EDIWriterException("Validation Error", e);
			}

			if (ediString != null) {
				// Save to EDIAnalyticsData Service
				createEDIAnalyticsDataSuccess(ediString, headerValues);

				JsonNode config = ediConfigClient.getEdiConfigBySenderCodeAndReceiverCode(ediWriterPayload.getPartnerName())
						.block();				
				if (StringUtils.isNotBlank(documentType)) {
					boolean isGroup = false;
					String attributeName = "isGrouped" + documentType;
					if(config != null && config.get(attributeName) != null && config.get(attributeName).isValueNode()) {
						isGroup = Boolean.parseBoolean(config.get(attributeName).textValue());
					}					 
					if (isGroup) {
						// Save for grouping
						ediGrouperService.saveEDIDocForGrouping(ediWriterPayload, headerValues, config);
					} else {
						// Push to GCS vis GCS Fuse
						String fileName = String.format(("%s/%s/%s/%s_%s.edi"), fusePath, ediWriterPayload.getPartnerName(),
								documentType, ediWriterPayload.getFileName(), Instant.now().toEpochMilli());
						Files.write(Paths.get(fileName), ediString.getBytes(StandardCharsets.UTF_8));
					}
					ediWriterMetrics.increaseTotalMsgCreationCount(documentType);
				}
				
				if(StringUtils.equals(documentType, "810")) {
					ediWriterMetrics.writeInvoiceMetricsFor810(headerValues, ediWriterPayload.getPartnerName());
				}
			} else {
				ediWriterMetrics.increaseTotalMsgCreationFailureCount(documentType);
				createEDIAnalyticsDataError(new EDIWriterException("Got Null after converting the Raw EDI data"),
						headerValues);
			}
		} catch (Exception e) {
			log.error("Ediwriter processing error", e);
			ediWriterMetrics.increaseTotalMsgCreationFailureCount(documentType);
			try {
				createEDIAnalyticsDataError(e, headerValues);
			} catch (Exception e2) {
				log.error("Error saving the Error EDI data to EDIANALYTICS DB", e);
			}
		}
		return ediString;
	}

	public String convertJsonToEDI(EDIWriterPayload ediWriterPayload) {
		String ediString = null;
		JsonNode jsonData = ediWriterPayload.getData();
		boolean isEnvPresent = false;
		try (StringReader reader = new StringReader(jsonData.toString()); StringWriter writer = new StringWriter();) {
			EnvelopeSpecification env = new EnvelopeSpecificationImpl();

			if (ediWriterPayload.getRepetitionSeparator() != null) {
				env.setRepetitionSeparator(ediWriterPayload.getRepetitionSeparator());
				isEnvPresent = true;
			} else {
				env.setRepetitionSeparator(0);
			}

			if (ediWriterPayload.getDelimiter() != null) {
				env.setDelimiter(ediWriterPayload.getDelimiter().charAt(0));
				isEnvPresent = true;
			}
			if (ediWriterPayload.getSubDelimiter() != null) {
				env.setSubDelimiter(ediWriterPayload.getSubDelimiter().charAt(0));
				isEnvPresent = true;
			}
			if (ediWriterPayload.getTerminator() != null) {
				env.setTerminator(ediWriterPayload.getTerminator().charAt(0));
				isEnvPresent = true;
			}
			JsonToEdi jsonToEdi = new JsonToEdi();
			if (isEnvPresent) {
				jsonToEdi.asEdi(reader, env, writer);
			} else {
				jsonToEdi.asEdi(reader, writer);
			}
			ediString = writer.toString();
		} catch (Exception e) {
			log.error("Error converting EDI Json to raw EDI document", e);
			throw new EDIWriterException("Error converting EDI data to Json", e);
		}
		return ediString; 
	}

	private void createEDIAnalyticsDataSuccess(String ediString, Map<String, String> headerValues) throws Exception {
		EDIData ediAnalyticsData = new EDIData();
		ediAnalyticsData.setTraceId(ediReaderUtil.getTraceId());
		ediAnalyticsData.setLastProcessStage(appName);
		ediAnalyticsData.setStatus("2xx");
		ediAnalyticsData.setProcessedData(ediString);
		ediAnalyticsData.setSourceTopic(null);
		ediAnalyticsData.setType(headerValues.get(EDIJsonHeaderUtil.KEY_TYPE));
		ediAnalyticsData.setSendercode(headerValues.get(EDIJsonHeaderUtil.KEY_SENDERCODE));
		ediAnalyticsData.setReceivercode(headerValues.get(EDIJsonHeaderUtil.KEY_RECEIVERCODE));
		ediAnalyticsDataClient.saveEDIData(Mono.just(ediAnalyticsData)).block();
	}

	private void createEDIAnalyticsDataError(Exception e, Map<String, String> headerValues) throws Exception {
		EDIData ediAnalyticsData = new EDIData();
		ediAnalyticsData.setTraceId(ediReaderUtil.getTraceId());
		ediAnalyticsData.setLastProcessStage(appName);
		ediAnalyticsData.setStatus("5xx");
		ediAnalyticsData.setProcessedData(null);
		ediAnalyticsData.setSourceTopic(null);
		ediAnalyticsData.setType(headerValues.get(EDIJsonHeaderUtil.KEY_TYPE));
		ediAnalyticsData.setSendercode(headerValues.get(EDIJsonHeaderUtil.KEY_SENDERCODE));
		ediAnalyticsData.setReceivercode(headerValues.get(EDIJsonHeaderUtil.KEY_RECEIVERCODE));
		ediAnalyticsData
				.setErrorMessage(e.getMessage() + (e.getCause() != null ? e.getCause().getLocalizedMessage() : ""));
		ediAnalyticsDataClient.saveEDIData(Mono.just(ediAnalyticsData)).block();
	}
}
