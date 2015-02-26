package com.mydomain.emailservice.service;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mydomain.emailservice.commons.JobRequest;
import com.mydomain.emailservice.commons.JobResponse;
import com.mydomain.emailservice.commons.ResultString;

public class EmailServiceIT {
    private static String endpointUrl;

    @BeforeClass
    public static void beforeClass() {
        endpointUrl = System.getProperty("service.url");
    }

    @Test
    public void testAddJobAndGetJob() throws Exception {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new org.codehaus.jackson.jaxrs.JacksonJsonProvider());
        
        JobRequest request = new JobRequest();
        request.setFrom("from@from.com");
        request.setTo("to@to.com");
        request.setSubject("Subject");
        request.setBody("Body");
        
        WebClient client = WebClient.create(endpointUrl + "/cxf/emailservice/emailjobs", providers);
        
        Response r = client.accept("application/json")
            .type("application/json")
            .post(request);
        
        assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
        
        MappingJsonFactory factory = new MappingJsonFactory();
        
        JsonParser parser = factory.createJsonParser((InputStream)r.getEntity());
        
        JobResponse response = parser.readValueAs(JobResponse.class);
                
        assertEquals(ResultString.QUEUED.getResultString(), response.getResult());
        
        // Test getJob
        System.out.println("Waiting for response...");
        int retry = 30;
        JobResponse responseGet;
        
        do
        {
			Thread.sleep(2000);
	        
	        WebClient clientGet = WebClient.create(endpointUrl + "/cxf/emailservice/emailjobs/" + response.getId(), providers);
	               
	        Response rGet = clientGet.accept("application/json").get();
	        
	        JsonParser parserGet = factory.createJsonParser((InputStream)rGet.getEntity());
	        
	        responseGet = parserGet.readValueAs(JobResponse.class);
	        
	        if(!ResultString.QUEUED.getResultString().equals(responseGet.getResult())) break;
	        
	        retry--;
        }while(retry>0);    
        
        System.out.println(responseGet.getMessage());
        assertEquals(ResultString.SUCEEDED.getResultString(), responseGet.getResult());
    }
}
