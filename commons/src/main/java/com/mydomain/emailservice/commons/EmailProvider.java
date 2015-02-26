package com.mydomain.emailservice.commons;

public interface EmailProvider {
	public String getName();
	public JobResponse sendEmail(JobRequest jobRequest);
}
