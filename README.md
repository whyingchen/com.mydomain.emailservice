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

Emailservice is a stateless serive, which can be deployed on multiple machines to support more clients. A load balancer can be placed before the Emailservice mechines and it can ping each instance for health check. According to a health measurement, the load balancer can distribute workload evenly.

### Usablity

EmailSerivce holds local job queues in memory. This is a very fast to store, enque or deque jobs. However, if a machine is down due to software or hardware problmes, the jobs on the machine will be lost.

One soultion is to use a persistent storeage to store a gloal job queue and each service instance will deque and enque remotely. If one machine is down, the jobs in queues are still there and other machines can work on jobs. The disadvantage of a remote global queue is extra overhead of network and persistent store.

### Persistent Storeage

#### Partition
For a persistent storeage, we need to partition data across multiple data nodes. In email service, we can hash job ids into a big number and then mod the number of the nodes and store the on that node. This is fast to locate the node for a job, however if we add or remove nodes from the cluster, we have to move re-hash job id and move jods across nodes. This is time consuming.

Another way is to use consistent hashing. We can convert a jod id into a key in a very large hash space and assign a key to each node. The job will be store on the node whose key is closest to the job key. It is like a ring and we can find the next node key from the job key moving close-wise on the ring. When we add or remove nodes, only the nodes around the new or removed nodes need to be moved.

#### Master-slave or P2P
To manage the nodes in a persistent storeage, we may use master-slave mode or P2P mode.

In a master-slave mode, clients always contact with the master node to find our where are the data and then contact with slave node foe the data. The master node may have a map between a job id to a node id and maintain the map when one node is added to removed. This is a fast way to locate the data, however the master node is single point of failture, if master node is down, the cluster is useless.

In a P2P mode, clients can contact any node for a job, that node will work a coordinator and try to find where are the data. It can contact with its neighbours for the data and eventually find a node, which has the data. In P2P, there is no SPOF, however it may take some hops across nodes to find the data.


#### Replication

Any node can fail in the cluster, we have to keep more copies across multiple nodes. We can pick nodes in different data center or different rack to store copies. Therefore the chance when all copies fail is low. However replication is trade-off for consistency.

#### Consistency

when there are more copies in the cluster, when we write data into one node, the data will take a while to be copied to other nodes. During this period, different nodes may have different copies and clients may not read the latest copy. The soution would be to wait for all nodes get the new data before finish a write opration. Or when read, we read the data from all copies and the latest copy wins according timestamp. In both cases, performance will be affected. In emailservice, if we do not care the updated email job, we can just write or read any node. This will be much fast.

#### Data Model

The next thing we need to consider is the data model. How do we store job queues? Use relational database, key value DB, big table DB, document DB or file systems.
They have pros and cons and apply for different applications. In email service, we may use file systems. When we enque, we append a job at the end of a file, when we deque, we read file for next job and save the offset for the next read. It is easy and fast than large DB systems.

### Monitoring

We need to monitor our service using metrics, which can be uploaded to graphite through statd. We can check CPU, memory usage or other timer, count in email service.


## Implemntation

The current implementation is only for a prototype. I choose existing tools and technologies.

To implemnt service, I use CXF and spring framework in Java, they provide rich functions for REST services and clients. Sprint also provides object injection. This is very usful for extend or functions without changing codes. For example, if we want to change email provider, we can change the bean setting in context files. Object injection can also be used in unit tests to provide mock email providers.

To implment frontend, I just use javascript and jquery lib to call REST APIs. the web site is very simple, since I focus on the backend service in this project. 

I also use Tomcat as servlet container and use EC2 to host my service.

## Tests

Due to the limit time, I only provide an intergration test for POST and GET APIs. If I have more time,  I will add more unit test using mock objects and add some intergration tests for email providers.


## Deployment

I have deployed the service on 

http://ec2-52-11-25-114.us-west-2.compute.amazonaws.com:8080/

It provides functions to send emails and check job stutus in a brower.

## Feedback

Thank you for reading this doc.
Your feedback is highly appreciated!
 











