package com.mydomain.emailservice.emailprovider;

import java.util.List;

public class SendGridResponse {
	
	private String message;
	private List<String> errors;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<String> getErrors() {
		return errors;
	}
	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
	
}
