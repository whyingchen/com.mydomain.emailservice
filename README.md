# EmailService

## Intruduction
EmailService is a REST serivce, which accepts a from email address, a to email address, a subject and an email body, and then sends the email through a 3rd party email providers, such as "sendgrid" and "email gun". If one email provider is down, it will fail over to the second one automatically.

## Design

### REST APIs
EmailService is a stateless backend service, which exposes two REST APIs. Both APIs use JSON input and output.

EmailService uses email jobs as the resoures and provide POST and GET methods to add and get resources.

* POST

Post API will add an email job and return the status and a job ID for later queries.

The URI for POST cxf/emailservice/emailjobs

The request is in JSON format:
```
{
"from":"a@a.com",
"to":"b@b.com",
"subject":"Email Subject",
"body":"Email Body"
}
```
The response is like:
```
{
"message":"Job is inqueue",
"id":"63310b92-52a2-4ebc-b46d-0ccbd023c335",
"result":"Queued"
}
```
The status code will be 200 for success or 4XX for errors in HTTP response.

* GET

GET API provides a way to retrive the status of a job.
The uri is like:

cxf/emailservice/emailjobs/f4526575-9b97-4b8f-ad48-ede3c9b2285e

The request for GET is empty and the response is in JSON format like:
```
{
"message":"Suceeded from sendgrid message=success",
"id":"63310b92-52a2-4ebc-b46d-0ccbd023c335",
"result":"Suceeded"
}
```

There are a couple of future APIs such as

* PUT (TODO)

This API provides a way to update an email job if the job is still in the queue.

* DELETE (TODO)
This API provide a way to cancel an email job if the job is still in the queue.  
### Queues and Hashmap

EmailService APIs are async APIS, which mean they will return immediately and clients can check the status later. The pros for async calls is the service accepts a large amount request at the peak and work on these jobs later. The cons is that we will need extra memory to store jobs.

There are two queues "pendingJobs", "finishedJobs" and a Hashmap to convert a job ID to a job.

* pendingJobs

POST API will create an email job and put the job in the queue before return. A job ID(UUID) is also generated at that time and attached with the job. The id and the job are store in a hashmap for later use. 

* finishedJobs

After a job is done, it will be moved from pendingJobs to finishedJobs.

* Hashmap

Hashmap is used to convert a job ID to job for GET API to reteive the job status.

### Background Worker Threads

Emailservice uses some background worker threads to work on email jobs and enque and deque on queues.

* sendJobWorkers

sendJobWorkers are some thread in a fixed size threading pool. The workers will deque jobs from pendingJobs and submit them to 3rd party email providers. The job is done, the job status will be updated and the job will be moved to finishedJobs. 

sendJobWorkers will be in waiting status if pendingJobs is empty, so that a little resources are used when system idles.

* removeJobWorkers

removeJobWorkers are some thread in a fixed size threading pool. The workers are run at the certain interval. The worker will remove jobs from finishedJobs when jobs are old enough based on a TTL(time to life). Therefore the service can free some memory for new jobs.

### EMail Provider
The emailserivce has an interface for email providers
```
public interface EmailProvider {
	public String getName();
	public JobResponse sendEmail(JobRequest jobRequest);
}
```

There are two implmentations so far: sendgrid and mailgun.
It is easy to add more email providers.

## Design Considerations

The current design is for a protocol system and there are many considerations to convert it to a product system. Here is a list of future work.

### Scalability

Emailservice is a statless serive, which can be deployed on multiple machines to support more clients. A load balancer can be placed before the Emailservice mechines and it can ping each instance for health check. Depends on a health measurement, the load balancer can distribute workload evenly.

### Usablity

EmailSerivce holds job queues in memory. This is a very fast to store and enque or deque jobs. However, if the machine is down due to software or hardware problmes, the jobs on the machine will be lost.

One soultion is to use a persistent storeage to store a gloal job queue and each service instance will deque and enque remotely. If one machine is down, the jobs in queues are still there and other machines can work on jobs. The disadvantage of a remote global queue is extre overhead of network and persistent store.

### Persistent Storeage


