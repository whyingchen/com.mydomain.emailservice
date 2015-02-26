package com.mydomain.emailservice.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RemoveJobWorker extends Thread{

	private JobQueue finishedJobs;
	private long ttlSecond;
	private ConcurrentHashMap<String,EmailJob> id2Job;
	
	public RemoveJobWorker(JobQueue finishedJobs, long ttlSecond, ConcurrentHashMap<String,EmailJob> id2Job)
	{
		this.finishedJobs = finishedJobs;
		this.ttlSecond = ttlSecond;
		this.id2Job = id2Job;
	}
	
	@Override
	public void run()
	{
		while(!Thread.currentThread().isInterrupted())
		{
			List<EmailJob> jobs = finishedJobs.removeOldJob(ttlSecond);
			for(EmailJob job:jobs)
			{
				id2Job.remove(job.getId());
			}
		}
	}

}