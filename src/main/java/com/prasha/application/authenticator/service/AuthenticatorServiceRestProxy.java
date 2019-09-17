package com.prasha.application.authenticator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jsonwebtoken.Claims;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*TODO: Use slf4j instead of java logger*/
import java.util.logging.Logger;

@Path("/authenticate")
public class AuthenticatorServiceRestProxy {
	private static AuthenticatorServiceImpl authenticatorServiceImpl = AuthenticatorServiceImpl.getInstance();
	private static final Logger log = Logger.getLogger(AuthenticatorServiceRestProxy.class.getName());

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response generateJWTTokenForUser(@HeaderParam("Authorization") String authorizationString) {
		String[] authParts = authorizationString.split("\\s+");
		String requestCredential = authParts[1];
		if (authenticatorServiceImpl.generateUserAccessToken(requestCredential) == null) {
			/*TODO: Create custom exception handler for error responses*/
			return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
		}

		return Response.ok(authenticatorServiceImpl.generateUserAccessToken(requestCredential).toString()).build();
	}

	@GET
	@Path("/validate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerDevice(@HeaderParam("Authorization") String authorizationHeader) {
		if (authorizationHeader == null || authorizationHeader.isEmpty()) {
			return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
		}

		String token = authorizationHeader.substring("Bearer".length()).trim();
		log.info(String.format("Token is: %s", token));
		Claims claimsBody = authenticatorServiceImpl.validateToken(token);
		if (claimsBody == null) {
			return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
		}

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jNode = mapper.createObjectNode();
		jNode.put("userName", claimsBody.getSubject());

		return Response.ok(jNode.toString()).build();
	}
}
