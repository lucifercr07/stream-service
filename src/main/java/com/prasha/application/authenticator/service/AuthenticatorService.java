package com.prasha.application.authenticator.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;

public interface AuthenticatorService {
	JsonNode generateUserAccessToken(String requestUsername);
	Claims validateToken(String jwt);
}
