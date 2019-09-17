package com.prasha.application.exception.service;

import javax.ws.rs.core.Response.Status;

public class StreamServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String message;
    private Status messageStatusCode;

    public Status getStatusCode(){
        return messageStatusCode;
    }

    public String getMessage(){
        return message;
    }

    public StreamServiceException() {
        super();
    }

    public StreamServiceException(final Status messageStatusCode) {
        super(messageStatusCode.toString());
        this.message = messageStatusCode.toString();
        this.messageStatusCode = messageStatusCode;
    }

    public StreamServiceException(final Status messageStatusCode, final String msg) {
        super(msg);
        this.message = msg;
        this.messageStatusCode = messageStatusCode;
    }
}
