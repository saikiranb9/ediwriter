package com.atd.microservices.core.ediwriter.exception;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EDIWriterException extends RuntimeException{
	
	private static final long serialVersionUID = -2589421071244904734L;
	
	private Date timestamp;
	private String message;
	
	public EDIWriterException(String message) {
		super(message);
		this.message = message;
	}
	
	public EDIWriterException(Date timestamp, String message) {
		super();
		this.timestamp = timestamp;
		this.message = message;
	}
	
	public EDIWriterException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}

}
