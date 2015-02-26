package com.mydomain.emailservice.commons;


public class JobResponse {
    private String result;
    private String message;
    private String id;
    
    public JobResponse()
    {
    	this.result = "";
    	this.message = "";
    	this.id = "";
    }

	public JobResponse(String result, String message)
    {
    	this.result = result;
    	this.message = message;
    }
	
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
    
    public String getResult() {
        return result;
    }

    public void setResult(String result ) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
