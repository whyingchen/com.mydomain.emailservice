package com.mydomain.emailservice.service;

import java.util.Date;

import com.mydomain.emailservice.commons.JobResponse;
import com.mydomain.emailservice.commons.ResultString;
import com.mydomain.emailservice.commons.EmailProvider;
import com.mydomain.emailservice.service.EmailJob.JobStatus;

public class SendJobWorker extends Thread{

	private JobQueue pendingJobs;
	private JobQueue finishedJobs;
	private EmailProvider primary;
	private EmailProvider failover;

	public SendJobWorker(JobQueue pendingJobs, JobQueue finishedJobs, EmailProvider primary, EmailProvider failover)
	{
		this.pendingJobs = pendingJobs;
		this.finishedJobs = finishedJobs;
		this.primary = primary;
		this.failover = failover;
	}
	
	@Override
	public void run()
	{
		while(!Thread.currentThread().isInterrupted())
		{
			EmailJob job = pendingJobs.removeJob();
			if(job!=null)
			{
				JobResponse response = primary.sendEmail(job.getRequest());
				
				if(!response.getResult().equals(ResultString.SUCEEDED.getResultString()))
				{
					response = failover.sendEmail(job.getRequest());
				}
				
				job.setResponse(response);
				job.setTimeStampWhenFinished(new Date());
				job.setStatus(JobStatus.FINISHED);
				finishedJobs.addJob(job);
			}
		}
	}

}
