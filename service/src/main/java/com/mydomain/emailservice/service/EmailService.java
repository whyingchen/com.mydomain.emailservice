package com.mydomain.emailservice.service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.mydomain.emailservice.commons.JobRequest;
import com.mydomain.emailservice.commons.JobResponse;
import com.mydomain.emailservice.commons.ResultString;
import com.mydomain.emailservice.emailprovider.EmailProviderMailGun;
import com.mydomain.emailservice.emailprovider.EmailProviderSendGrid;

@Path("/emailservice")
public class EmailService {

	private JobQueue pendingJobs;
	private JobQueue finishedJobs;
	private ConcurrentHashMap<String, EmailJob> id2Job;
	private ExecutorService  sendJobWorkers;
	private ScheduledExecutorService  removeJobWorkers; 
	
	private void startSendJobWorkers(int numThread)
	{
		sendJobWorkers = Executors.newFixedThreadPool(numThread);
		for(int i=0;i<numThread;i++)
		{
			sendJobWorkers.execute(new SendJobWorker(pendingJobs,finishedJobs, new EmailProviderSendGrid(), new EmailProviderMailGun()));
		}
	}

	private void startRemoveJobWorkers(int numThread, long ttlSecond, long intervalSecond)
	{
		removeJobWorkers = Executors.newScheduledThreadPool(numThread);
		for(int i=0;i<numThread;i++)
		{
			removeJobWorkers.scheduleWithFixedDelay(new RemoveJobWorker(finishedJobs, ttlSecond, id2Job), i, intervalSecond, TimeUnit.SECONDS);
		}		
	}
	
	public EmailService()
	{
		pendingJobs = new JobQueue(1000);
		finishedJobs = new JobQueue(1000);
		id2Job = new ConcurrentHashMap<String, EmailJob>();
		startSendJobWorkers(5);
		startRemoveJobWorkers(1,60*60,60);
	}
	
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Path("/emailjobs")
    public Response addJob(JobRequest request) {  
    	    	
    	EmailJob emailJob = new EmailJob(request);
    	if(!pendingJobs.addJob(emailJob))
    	{
    		JobResponse response = new JobResponse(ResultString.FAILED.getResultString(), "Server is busy");
    		return Response.serverError().entity(response).build();
    	}
    	else
    	{
    		JobResponse response = new JobResponse(ResultString.QUEUED.getResultString(), "Job is in queue");
    		response.setId(emailJob.getId());
    		id2Job.put(emailJob.getId(), emailJob);
    		return Response.ok().entity(response).build();
    	}
    }
    
    @GET
    @Path("/emailjobs/{input}")
    @Produces("application/json")
    public Response getJob(@PathParam("input") String input) {
    	
    	if(!id2Job.containsKey(input))
    	{
    		JobResponse response = new JobResponse(ResultString.FAILED.getResultString(),"Job not found");
    		return Response.noContent().entity(response).build();    		
    	}
    	else
    	{
    		EmailJob job = id2Job.get(input);
    		if(job.getStatus() == EmailJob.JobStatus.QUEUEED)
    		{
        		JobResponse response = new JobResponse(ResultString.QUEUED.getResultString(), "Job is in queue.");
        		response.setId(job.getId());
        		return Response.ok().entity(response).build();
    		}
    		else if(job.getStatus() == EmailJob.JobStatus.FINISHED)
    		{
        		JobResponse response = job.getResponse();
        		response.setId(job.getId());
        		return Response.ok().entity(response).build();
    		}
    		else
    		{
        		JobResponse response = new JobResponse(ResultString.FAILED.getResultString(), "Unknown status");
        		response.setId(job.getId());
        		return Response.ok().entity(response).build();    			
    		}
    	}
    }    
}

