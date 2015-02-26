package com.mydomain.emailservice.commons;

public enum ResultString
{
	SUCEEDED("Suceeded"),
	FAILED("Failed"),
	QUEUED("Queued");
	
	private String resultString;
	
	private ResultString(String resultString)
	{
		this.resultString = resultString;
	}
	
	public String getResultString()
	{
		return resultString;
	}
}
