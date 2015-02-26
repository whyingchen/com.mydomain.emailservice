package com.mydomain.emailservice.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class JobQueue {
	
	private LinkedList<EmailJob> queue;
	private int maxSize;
	
	public JobQueue(int maxSize)
	{
		this.maxSize = maxSize;
		queue = new LinkedList<EmailJob>();
	}
	
	public boolean addJob(EmailJob job)
	{
		// We do not want to block service threads
		if(queue.size() >= maxSize) return false;
		
		synchronized(queue)
		{
			while(queue.size()>=maxSize)
			{
				try {
					queue.wait();
				} catch (InterruptedException e) {
					// Restore flag so that caller can test it
					Thread.currentThread().interrupt();
					return false;
				}
			}
			queue.addLast(job);
			queue.notifyAll();
		}
		
		return true;
	}

	public EmailJob removeJob()
	{
		EmailJob job = null;
		synchronized(queue)
		{
			while(queue.size()==0)
			{
				try {
					queue.wait();
				} catch (InterruptedException e) {
					// Restore flag so that callers can test it
					Thread.currentThread().interrupt();
					return null;
				}
			}
			job = queue.removeFirst();
			queue.notifyAll();
		}
		
		return job;		
	}
	
	public List<EmailJob> removeOldJob(long ttlSecond)
	{
		List<EmailJob> jobs = new ArrayList<EmailJob>();

		synchronized(queue)
		{
			while(queue.size()==0)
			{
				try {
					queue.wait();
				} catch (InterruptedException e) {
					// Restore flag so that callers can test it
					Thread.currentThread().interrupt();
					return null;
				}
			}
			
			// Remove ttlSecond old jobs since finished			
			while(queue.size()!=0)
			{
				EmailJob job = queue.getFirst();
				long diffInMillies =(new Date()).getTime() - job.getTimeStampWhenFinished().getTime();				
				if(diffInMillies/1000 >= ttlSecond)
				{
					queue.removeFirst();
					jobs.add(job);
				}
				else break;
			}
			
			queue.notifyAll();
		}
		
		return jobs;
	}	
}
