package com.atd.microservices.core.ediwriter;

import com.atd.microservices.core.ediwriter.utils.EDIFileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import brave.Span.Kind;
import brave.baggage.BaggagePropagation;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Map;


@SpringBootApplication(scanBasePackages = {"com.atd.utilities.kafkalogger", "com.atd.microservices.core.ediwriter"})
@EnableSwagger2
@EnableAsync
public class EDIWriterApplication {

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	EDIFileUtils ediFileUtils;

	@Value("${env.host.url:#{null}}")
	private String envHostURL;

	public static void main(String[] args) {
		SpringApplication.run(EDIWriterApplication.class, args);
	}

	/*@Bean
	public TraceableExecutorService getExecutorService(BeanFactory beanFactory) {
		return new TraceableExecutorService(beanFactory,
        		Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), "futureroutes");
	}*/
	
	@Bean
	public WebClient getWebClientBuilder(){
		return WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer -> configurer
								.defaultCodecs()
								.maxInMemorySize(20 * 1024 * 1024))
						.build())
				.build();
	}
	
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
	
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

//	@Bean
//	public Map<String, Map<String, String>> modelResolverMap() {
//		return ediFileUtils.getModelResolverMap();
//	}
	
	static final Propagation.Factory B3_FACTORY = B3Propagation.newFactoryBuilder()
			.injectFormat(Kind.PRODUCER, B3Propagation.Format.MULTI).build();

	@Bean
	BaggagePropagation.FactoryBuilder baggagePropagationFactoryBuilder() {
		return BaggagePropagation.newFactoryBuilder(B3_FACTORY);
	}
}
