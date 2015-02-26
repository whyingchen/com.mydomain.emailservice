package com.mydomain.emailservice.service;

import java.util.Date;
import java.util.UUID;

import com.mydomain.emailservice.commons.JobRequest;
import com.mydomain.emailservice.commons.JobResponse;

public class EmailJob {
	private JobRequest request;
	private JobResponse response;
	private UUID id;
	private JobStatus status;
	private Date timeStamp;
	private Date timeStampWhenFinished;
	
	public enum JobStatus
	{
		QUEUEED,
		FINISHED;
	}

	public Date getTimeStampWhenFinished() {
		return timeStampWhenFinished;
	}

	public void setTimeStampWhenFinished(Date timeStampWhenFinished) {
		this.timeStampWhenFinished = timeStampWhenFinished;
	}
	
	public JobResponse getResponse() {
		return response;
	}

	public void setResponse(JobResponse response) {
		this.response = response;
	}
		
	public JobRequest getRequest()
	{
		return request;
	}
	
	public Date getTimeStamp()
	{
		return timeStamp;
	}
	
	public String getId()
	{
		return id.toString();
	}
	
	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public EmailJob(JobRequest request)
	{
		this.request = request;
		timeStamp = new Date();
		id = UUID.randomUUID();
		status = JobStatus.QUEUEED;
	}
}
