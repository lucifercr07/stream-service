package com.prasha.application.authenticator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prasha.application.exception.service.StreamServiceException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*TODO: Use slf4j instead of java logger*/
import java.io.IOException;
import java.util.logging.Logger;

import static com.prasha.application.CommonUtils.*;

@Path("/user")
public class AuthenticatorServiceRestProxy {
	private static AuthenticatorService authenticatorServiceImpl = AuthenticatorServiceImpl.getInstance();
	private static final Logger log = Logger.getLogger(AuthenticatorServiceRestProxy.class.getName());
	private static ObjectMapper mapper = new ObjectMapper();

	@GET
	@Path("/authenticate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response generateJWTTokenForUser(@HeaderParam("Authorization") String authorizationString) {
		validateRequestString(authorizationString);
		String[] authParts = authorizationString.split("\\s+");
		String requestCredential = authParts[1];
		if (authenticatorServiceImpl.generateUserAccessToken(requestCredential) == null) {
			return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
		}

		return Response.ok(authenticatorServiceImpl.generateUserAccessToken(requestCredential).toString()).build();
	}

	@POST
	@Path("/register")
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerUser(String requestString) {
		validateRequestString(requestString);
		try {
			JsonNode requestJson;
			requestJson = mapper.readTree(requestString);
			authenticatorServiceImpl.createUser(requestJson);
		} catch (IOException e) {
			throw new StreamServiceException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.ok().build();
	}

	@GET
	@Path("/validate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerDevice(@HeaderParam("Authorization") String authorizationHeader) {
		validateRequestString(authorizationHeader);
		String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
		String userName = authenticatorServiceImpl.validateToken(token);

		ObjectNode jNode = mapper.createObjectNode();
		jNode.put(USERNAME, userName);

		return Response.ok(jNode.toString()).build();
	}

	private void validateRequestString(final String requestString) {
		if (requestString == null || requestString.isEmpty()) {
			throw new StreamServiceException(Response.Status.BAD_REQUEST);
		}
	}
}
