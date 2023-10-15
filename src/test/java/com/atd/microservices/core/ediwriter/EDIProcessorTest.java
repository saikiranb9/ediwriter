package com.atd.microservices.core.ediwriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xml.sax.InputSource;

import com.atd.microservices.core.ediwriter.domain.EDIWriterPayload;
import com.atd.microservices.core.ediwriter.service.EDIGrouperService;
import com.atd.microservices.core.ediwriter.service.EDIProcessor;
import com.atd.microservices.core.ediwriter.webclients.EDIAnalyticsDataClient;
import com.berryworks.edireader.EDIReader;
import com.berryworks.edireader.EDIReaderFactory;
import com.berryworks.edireader.EDISyntaxException;
import com.berryworks.edireader.json.fromedi.EdiToJson;
import com.berryworks.edireader.json.toedi.JsonToEdi;
import com.berryworks.ediwriter.envelope.EnvelopeSpecification;
import com.berryworks.ediwriter.envelope.EnvelopeSpecificationImpl;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
		"kafka.bootstrap.server.url=null",
		"kafka.security.protocol=null",
		"ediwriter.kafka.topic.inbound=TEST_TOPIC",
		"ediwriter.kafka.topic.outbound=TEST_TOPICS",
		"ssl.truststore.password=null",
		"ssl.truststore.location=null",
		"kafka.analytic.topic=null",
		"ediwriter.ediAnalyticsDataUrl=null",
		"ediwriter.ediConfigUrl: http://apigateway:9093/ediconfig/partner/{partner}",
		"ediwriter.saveSingledocUrl: http://apigateway:9093/edicoredata/edidoc/edidoc/save"})
public class EDIProcessorTest {
   
	@Autowired
	private EDIProcessor ediProcessor;
	
	@MockBean
	private EDIAnalyticsDataClient ediAnalyticsDataClient;
	
	@MockBean
	private EDIGrouperService ediGrouperService;
	
    @Test
	public void testEDIWriter() throws Exception {
		Resource resource = new ClassPathResource("edi_processed_data_850.json");		

		try (InputStreamReader reader = new InputStreamReader(resource.getInputStream());
				StringWriter writer = new StringWriter()) {
			EnvelopeSpecification env = new EnvelopeSpecificationImpl();
			env.setDelimiter('*');
			env.setSubDelimiter('>');
			env.setTerminator('~');
			JsonToEdi jsonToEdi = new JsonToEdi();
			jsonToEdi.asEdi(reader, env, writer);
			String ediString = writer.toString();
			log.info(ediString);
			
			try {
				EdiToJson ediToJson = new EdiToJson();
				String json = ediToJson.asJson(ediString);
				log.info(json);
			} catch (Exception e) {
				log.error("Error converting EDI data to Json", e);
				//throw new EDIReaderException("Error converting EDI data to Json", e);
			}
		}
	}
    
    @Test
	public void testProcessEDIJson() throws InterruptedException, IOException, EDISyntaxException {
		Resource resource = new ClassPathResource("edieader_incoming_payload.json");
		JsonMapper mapper = JsonMapper.builder().configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true).build();

		EDIWriterPayload incomingEDIPayload = mapper.readValue(resource.getFile(), EDIWriterPayload.class);
		String ediString = ediProcessor.process(incomingEDIPayload);
		Assert.assertNotNull(ediString);

		EDIReader ediReader = EDIReaderFactory.createEDIReader(new InputSource(new StringReader(ediString)));
		Assert.assertEquals(ediReader.getDelimiter(), '*');
		Assert.assertEquals(ediReader.getSubDelimiter(), '>');
		Assert.assertEquals(ediReader.getTerminator(), '~');
	}
    
    @Test
	public void testExtractInvoice() throws IOException {

		Resource resource = new ClassPathResource("ediwriterincomingPayload_810.json");
		JsonMapper mapper = JsonMapper.builder().configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true).build();
		EDIWriterPayload incomingEDIPayload = mapper.readValue(resource.getFile(), EDIWriterPayload.class);
		JsonNode rootNode = incomingEDIPayload.getData();

		JsonNode segmentNode = rootNode.at("/interchanges/0/functional_groups/0/transactions/0/segments");
		String invoiceType = null;
		Double invAmount = null;
		// Type
		JsonNode invTypeNode = segmentNode.findValue("BIG_02");
		if (invTypeNode != null && !invTypeNode.isMissingNode()) {
			invoiceType = StringUtils.substring(invTypeNode.textValue(), 0, 1);
		}

		// Amount
		JsonNode amntNode = segmentNode.findValue("TDS_01");
		if (amntNode != null && !amntNode.isMissingNode()) {
			String amount = amntNode.textValue();
			if (StringUtils.isNotBlank(amount)) {
				invAmount = Double.parseDouble(amount) / 100;
			}
		}		
		log.info("Type:{}, Amount:{}", invoiceType, invAmount);
		Assert.assertEquals(invoiceType, "S");
		Assert.assertEquals(invAmount.toString(), "11.29");

	}

}