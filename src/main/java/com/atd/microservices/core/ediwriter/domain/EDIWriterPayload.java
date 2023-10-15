package com.atd.microservices.core.ediwriter.domain;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public class EDIWriterPayload {	
	
	@ApiModelProperty(required = false)
	private String terminator;
	
	@ApiModelProperty(required = false)
	private String delimiter;
	
	@ApiModelProperty(required = false)
	private String subDelimiter;
	
	@ApiModelProperty(required = false)
	private Integer repetitionSeparator;
	
	@ApiModelProperty(required = true)
	@NotNull
	private String fileName;
	
	@ApiModelProperty(required = true)
	@NotNull
	private String partnerName;
	
	@ApiModelProperty(required = true)
	@NotNull
	private JsonNode data;
	
	
}