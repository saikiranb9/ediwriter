package com.atd.microservices.core.ediwriter.webclients;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.atd.microservices.core.ediwriter.domain.EDIDoc;
import com.atd.microservices.core.ediwriter.exception.EDIWriterException;

import reactor.core.publisher.Mono;

@Component
public class EDICoreDataClient {
	
	@Autowired
	private WebClient webClient;

	@Value("${spring.application.name}")
	private String applicationName;

	@Value("${ediwriter.saveSingledocUrl}")
	private String saveSingledocUrl;
	
	public Mono<EDIDoc> saveSingleEDIDoc(EDIDoc ediDoc) {
		
		return webClient.post()
				.uri(URI.create(String.format(saveSingledocUrl)))
				.header("XATOM-CLIENTID", applicationName)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just(ediDoc), EDIDoc.class)
				.retrieve()
				.onStatus(HttpStatus::isError,
						exceptionFunction -> Mono.error(
								new EDIWriterException("EDIReader returned error attempting to call save Single EDIDoc")))
				.bodyToMono(EDIDoc.class)
				.onErrorResume(e -> Mono.error(new EDIWriterException(
						"Error while invoking Save Single EDIDoc API", e)));		
	}

}
