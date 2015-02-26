package com.mydomain.emailservice.emailprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import com.mydomain.emailservice.commons.EmailProvider;
import com.mydomain.emailservice.commons.JobRequest;
import com.mydomain.emailservice.commons.JobResponse;
import com.mydomain.emailservice.commons.ResultString;


public class EmailProviderMailGun implements EmailProvider{
	
	@Override
	public String getName()
	{
		return "mailgun";
	}
	
	@Override
	public JobResponse sendEmail(JobRequest jobRequest)
	{		
		MultivaluedMap<String,String> request = new MetadataMap<String,String>();
		request.add("from", jobRequest.getFrom());
		request.add("to", jobRequest.getTo());
		request.add("subject", jobRequest.getSubject());
		request.add("text", jobRequest.getBody());
	
		Form data = new Form(request);
		
        List<Object> providers = new ArrayList<Object>();
        providers.add(new org.codehaus.jackson.jaxrs.JacksonJsonProvider());

        WebClient client = WebClient.create("https://api.mailgun.net/v2/sandbox82ed86bda6f743baabcec745f6f9d41d.mailgun.org/messages", providers, "api","key-ccd9c5590c66ef4e12b1112f6adb3913", null);
        
        Response r = client.accept("application/json").form(data);

        MailGunResponse response = null;
        
		try 
		{
		    MappingJsonFactory factory = new MappingJsonFactory();
		    JsonParser parser;				
		    parser = factory.createJsonParser((InputStream)r.getEntity());
			response = parser.readValueAs(MailGunResponse.class);
		} 
		catch (IOException e) 
		{
			response = new MailGunResponse();
			response.setMessage("Error while parsing response");
			response.setId("N/A");
		}

        JobResponse jobResponse;
        if(r.getStatus() == 200)
        {
            jobResponse = new JobResponse(ResultString.SUCEEDED.getResultString(), 
            		"Suceeded from " + getName() 
            		+ " message=" + response.getMessage()
            		+ " id=" + response.getId());
        }
        else
        {
            jobResponse = new JobResponse(ResultString.FAILED.getResultString(), 
            		"Failed from " + getName() 
            		+ " message=" + response.getMessage()
            		+ " id=" + response.getId());
        }
        
		return jobResponse;
	}
	

}
