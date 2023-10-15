package com.atd.microservices.core.ediwriter.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EDIDoc {
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private String id;
	private String customerName;
	private String type;
	private String version;
	private String docId;
	private String ediJson;
	private Boolean processed;
	private Date createTime;
	private String senderCode;
	private String receiverCode;
}
