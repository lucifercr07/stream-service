package com.prasha.application.authenticator.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface AuthenticatorService {
	JsonNode generateUserAccessToken(String requestUsername);
	String validateToken(String jwt);
	void createUser(JsonNode requestJson);
}
