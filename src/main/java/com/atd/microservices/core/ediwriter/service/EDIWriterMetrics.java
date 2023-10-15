package com.atd.microservices.core.ediwriter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EDIWriterMetrics {
	
	MeterRegistry meterRegistry;

	public EDIWriterMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}
	
	public void increaseTotalMsgCreationCount(String type) {
		try {
			this.meterRegistry.counter(String.format(("ediwriter_total_%s_docs_created")), type).increment();
		} catch (Exception e) {
		}	
	}
	
	public void increaseTotalMsgCreationFailureCount(String type) {
		try {
			this.meterRegistry.counter(String.format(("ediwriter_total_%s_docs_creation_failed")), type).increment();
		} catch (Exception e) {
		}	
	}

	public void increaseTotalTypeMsgCount(String type) {
		this.meterRegistry.counter(String.format(("ediwriter_total_outgoing_%s_edi_docs"), type)).increment();
	}
	
	@Async
	public void writeInvoiceMetricsFor810(final Map<String, String> headerValues, String customerName) {
		Double invAmount = null;
		String invoiceType = headerValues.get(EDIJsonHeaderUtil.KEY_INVOICE_TYPE);
		String amount = headerValues.get(EDIJsonHeaderUtil.KEY_INVOICE_AMOUNT);
		if (StringUtils.isNotBlank(amount)) {
			invAmount = Double.parseDouble(amount) / 100;
		}
		try {
			// Write
			if (invAmount != null) {
				List<String> tags = new ArrayList<>();
				if (StringUtils.isNotEmpty(invoiceType)) {
					tags.add("invoiceType");
					tags.add(invoiceType);
				}
				if (StringUtils.isNotEmpty(customerName)) {
					tags.add("customer");
					tags.add(customerName);
				}
				this.meterRegistry.counter("ediwriter_810_total_invoice_amount",
						tags.toArray(new String[tags.size()])).increment(invAmount);
			}
		} catch (Exception e) {
			log.error("Error creating metrics ediwriter_810_total_invoice_amount - {}", e.getMessage());
		}
	}
}
