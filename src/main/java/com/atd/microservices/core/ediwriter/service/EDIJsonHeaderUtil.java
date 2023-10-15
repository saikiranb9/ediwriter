package com.atd.microservices.core.ediwriter.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.atd.microservices.core.ediwriter.exception.EDIWriterException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EDIJsonHeaderUtil {

	public static String KEY_SENDERCODE = "sendercode";
	public static String KEY_RECEIVERCODE = "receivercode";
	public static String KEY_STANDARD = "standard";
	public static String KEY_VERSION = "version";
	public static String KEY_TYPE = "type";
	public static String KEY_INVOICE_NUMBER = "invoicenumber";
	public static String KEY_INVOICE_TYPE = "invoicetype";
	public static String KEY_INVOICE_AMOUNT = "invoiceamount";
	public static String KEY_ASN_NUMBER = "asnnumber";
	private static Pattern pattern = Pattern.compile("(^\\h*)|(\\h*$)");

	@Autowired
	private ObjectMapper objectMapper;

	public Map<String, String> extractHeaderInfoFromEDIDoc(JsonNode rootNode) {
		String type, version, standard, senderCode, receiverCode = null;	
		String invoiceAmount = null;
		String invoiceNumber = null;
		String invoiceType = null;
		String asnNumber = null;
		
		String senderPrefix = trim(rootNode.at("/interchanges/0/ISA_05_SenderQualifier").textValue());
		JsonNode senderCodeNode = rootNode.at("/interchanges/0/ISA_06_SenderId");
		String receiverPrefix = trim(rootNode.at("/interchanges/0/ISA_07_ReceiverQualifier").textValue());
		JsonNode receiverCodeNode = rootNode.at("/interchanges/0/ISA_08_ReceiverId");

		if (senderCodeNode != null && !senderCodeNode.isMissingNode()) {
			senderCode = senderPrefix + "-" + trim(senderCodeNode.textValue());
		} else {
			throw new EDIWriterException(
					"Type information(ISA_05_SenderQualifier / ISA_06_SenderId) not found in EDI data");
		}
		
		if (receiverCodeNode != null && !receiverCodeNode.isMissingNode()) {
			receiverCode = receiverPrefix + "-" + trim(receiverCodeNode.textValue());
		} else {
			throw new EDIWriterException(
					"Type information(ISA_07_ReceiverQualifier / ISA_08_ReceiverId) not found in EDI data");
		}		
		// Type
		JsonNode typeNode = rootNode
				.at("/interchanges/0/functional_groups/0/transactions/0/ST_01_TransactionSetIdentifierCode");
		if (typeNode != null && !typeNode.isMissingNode()) {
			type = typeNode.textValue().trim();
		} else {
			throw new EDIWriterException(
					"Type information(ST_01_TransactionSetIdentifierCode) not found in EDI data");
		}

		// Version
		JsonNode versionNode = rootNode.at("/interchanges/0/functional_groups/0/GS_08_Version");
		if (versionNode != null && !versionNode.isMissingNode()) {
			version = versionNode.textValue();
		} else {
			throw new EDIWriterException("Version information(GS_08_Version) not found in EDI data");
		}

		// Standard
		JsonNode standardNode = rootNode.at("/interchanges/0/functional_groups/0/GS_07_ResponsibleAgencyCode");
		if (standardNode != null && !standardNode.isMissingNode()) {
			String value = standardNode.textValue();
			standard = StringUtils.equals("X", value.trim()) ? "X12" : value.trim();
		} else {
			throw new EDIWriterException("Standard information(GS_07_ResponsibleAgencyCode) not found in EDI data");
		}
		
		// Invoice details (for 810)
		if(StringUtils.equals(type, "810")) {			
			JsonNode segmentNode = rootNode.at("/interchanges/0/functional_groups/0/transactions/0/segments");
			
			// Number & Type
			JsonNode invNode = segmentNode.findValue("BIG_02");
			if (invNode != null && !invNode.isMissingNode()) {
				invoiceNumber = invNode.textValue();
				invoiceType = StringUtils.substring(invNode.textValue(), 0, 1);
			}

			// Amount
			JsonNode amntNode = segmentNode.findValue("TDS_01");
			if (amntNode != null && !amntNode.isMissingNode()) {
				invoiceAmount = amntNode.textValue();
			}
		} 
		// ASN Number (for 856)
		else if(StringUtils.equals(type, "856")) {			
			JsonNode segmentNode = rootNode.at("/interchanges/0/functional_groups/0/transactions/0/segments");
			
			JsonNode asnNode = segmentNode.findValue("BSN_02");
			if (asnNode != null && !asnNode.isMissingNode()) {
				asnNumber = asnNode.textValue();
			}
		}
		
		Map<String, String> headers = new HashMap<>();
		headers.put(KEY_SENDERCODE, senderCode);
		headers.put(KEY_RECEIVERCODE, receiverCode);
		headers.put(KEY_STANDARD, standard);
		headers.put(KEY_VERSION, version);
		headers.put(KEY_TYPE, type);
		headers.put(KEY_INVOICE_NUMBER, invoiceNumber);
		headers.put(KEY_INVOICE_TYPE, invoiceType);
		headers.put(KEY_INVOICE_AMOUNT, invoiceAmount);
		headers.put(KEY_ASN_NUMBER, asnNumber);
		return headers;
	}
	
	private static String trim(String str) {
		return pattern.matcher(str).replaceAll("");
	}

}
