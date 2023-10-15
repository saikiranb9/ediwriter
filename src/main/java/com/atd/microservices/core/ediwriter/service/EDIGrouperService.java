package com.atd.microservices.core.ediwriter.service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atd.microservices.core.ediwriter.domain.EDIDoc;
import com.atd.microservices.core.ediwriter.domain.EDIWriterPayload;
import com.atd.microservices.core.ediwriter.webclients.EDICoreDataClient;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EDIGrouperService {
	
	@Autowired
	private EDICoreDataClient ediCoreDataClient;
	
	public void saveEDIDocForGrouping(EDIWriterPayload ediWriterPayload, Map<String, String> headerValues,
			JsonNode config) {
		String type = headerValues.get(EDIJsonHeaderUtil.KEY_TYPE);		
		String docId = null;
		if(StringUtils.equals(type, "810")) {
			docId = headerValues.get(EDIJsonHeaderUtil.KEY_INVOICE_NUMBER);
		} else if(StringUtils.equals(type, "856")) {
			docId = headerValues.get(EDIJsonHeaderUtil.KEY_ASN_NUMBER);
		}
		if(docId != null) {
			EDIDoc ediDoc = new EDIDoc();
			ediDoc.setDocId(docId);
			ediDoc.setCustomerName(ediWriterPayload.getPartnerName());
			String jsontr = ediWriterPayload.getData().toString();
			ediDoc.setCreateTime(Date.from(Instant.now()));
			ediDoc.setEdiJson(jsontr);
			ediDoc.setProcessed(false);
			ediDoc.setType(type);
			ediDoc.setVersion(headerValues.get(EDIJsonHeaderUtil.KEY_VERSION));
			ediDoc.setSenderCode(config.get("senderCode").textValue());
			ediDoc.setReceiverCode(config.get("receiverCode").textValue());
			ediCoreDataClient.saveSingleEDIDoc(ediDoc).subscribe(c -> log.debug("Saved EDIDoc for grouping"));
		}		
	}

}
