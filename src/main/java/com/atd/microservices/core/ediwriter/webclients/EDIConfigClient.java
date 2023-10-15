package com.atd.microservices.core.ediwriter.webclients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.atd.microservices.core.ediwriter.exception.EDIWriterException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

@Component
public class EDIConfigClient {

	@Autowired
	private WebClient webClient;

	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${ediwriter.ediConfigUrl}")
	private String ediConfigUrl;

	public Mono<JsonNode> getEdiConfigBySenderCodeAndReceiverCode(String customerName) {

		return webClient.get()
				.uri(ediConfigUrl, customerName)
				.header("XATOM-CLIENTID", applicationName).retrieve()
				.onStatus(HttpStatus::isError,
						exceptionFunction -> Mono
								.error(new EDIWriterException("EDIReader returned error attempting to call EdiConfig")))
				.bodyToMono(JsonNode.class)
				.onErrorResume(e -> Mono.error(new EDIWriterException("Error while invoking EDIConfig API", e)));
	}
}