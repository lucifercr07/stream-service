package com.prasha.application.exception.service;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class StreamServiceExceptionHandler implements ExceptionMapper<StreamServiceException> {
    @Override
    public Response toResponse(StreamServiceException exception) {
        return Response.status(exception.getStatusCode()).entity(exception.getMessage()).build();
    }
}
