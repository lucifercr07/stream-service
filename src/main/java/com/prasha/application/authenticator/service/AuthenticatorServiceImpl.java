package com.prasha.application.authenticator.service;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Logger;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prasha.application.database.service.DatabaseService;
import com.prasha.application.database.service.DatabaseServiceImpl;
import com.prasha.application.exception.service.StreamServiceException;
import io.jsonwebtoken.*;

public class AuthenticatorServiceImpl implements AuthenticatorService {

    private static final Logger log = Logger.getLogger(AuthenticatorServiceImpl.class.getName());
    private static final AuthenticatorServiceImpl AUTH_SERVICE_IMPL = new AuthenticatorServiceImpl();
    // TODO: there might be affects of declaring mapper as static check once
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String secretKey = "";
    // 30 days in seconds
    private static Long expiryInSecs = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(2592000L).toEpochSecond(ZoneOffset.UTC);
    private static final String APP_NAME = "stream-service";
    private static final String IDENTITY_STORE_DB = "identityStore";
    private static final String USERNAME = "userName";
    private static final String PASSWORD = "password";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    private static JsonNode createDefaultUserCredentials() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jNode = mapper.createObjectNode();
        jNode.put(USERNAME, DEFAULT_USERNAME);
        jNode.put(PASSWORD, DEFAULT_PASSWORD);
        return jNode;
    }

    private AuthenticatorServiceImpl() {
        // Currently creating default user at start for testing, very imp. to be removed
        DatabaseServiceImpl.getInstance().insertOne(IDENTITY_STORE_DB, createDefaultUserCredentials());
    }

    public static AuthenticatorServiceImpl getInstance() {
        return AUTH_SERVICE_IMPL;
    }

    @Override
	public JsonNode generateUserAccessToken(String requestCredential) {
        if (!verifyUserCredentials(requestCredential)) {
            throw new StreamServiceException(Response.Status.FORBIDDEN);
        }

        ObjectNode jNode = mapper.createObjectNode();
        String jwt = createJWTToken(APP_NAME, DEFAULT_USERNAME);
        validateToken(jwt);
        // Insert access token corresponding to user in db with expiry time
        return jNode.put(ACCESS_TOKEN, jwt);
	}

	/*TODO: Can change from random UUID to random base64 string?*/
	private String createJWTToken(String appName, String userName) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
	    if (secretKey == null || secretKey.isEmpty()) {
            this.secretKey = generateRandomKey();
        }

        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(this.secretKey);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        return Jwts.builder().setIssuer(appName)
                .setSubject(userName)
                .setExpiration(Date.from(Instant.ofEpochSecond(expiryInSecs)
                        .atOffset(ZoneOffset.UTC).toInstant()))
                .setIssuedAt(Date.from(LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)))
                .claim(USERNAME, userName)
                .signWith(SignatureAlgorithm.HS256, signingKey)
                .compact();
    }

    public Claims validateToken(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
                    .parseClaimsJws(jwt).getBody();
            return claims;
        } catch (ExpiredJwtException | SignatureException | MalformedJwtException e) {
            // TODO: Create custom exception handler
            log.severe(String.format("Invalid request", e.getMessage()));
        }
        return null;
    }

    private String generateRandomKey() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private String decoder(String credential) {
        return new String(Base64.getDecoder().decode(credential));
    }

    private boolean verifyUserCredentials(String requestCredential) {
        String decodedCredential = decoder(requestCredential);
        String userName = decodedCredential.split(":")[0];
        String password = decodedCredential.split(":")[1];

        if (!checkIfUserExists(userName)) {
            throw new StreamServiceException(Response.Status.NOT_FOUND);
        }

        ArrayNode array = mapper.createArrayNode();
        ObjectNode queryNode = mapper.createObjectNode();
        array.add(mapper.createObjectNode().put(USERNAME, userName));
        array.add(mapper.createObjectNode().put(PASSWORD, password));
        queryNode.put(DatabaseService.MONGO_AND_OPERATOR, array);

        return DatabaseServiceImpl.getInstance().findOne(IDENTITY_STORE_DB, queryNode) != null ? true : false;
    }

    private boolean checkIfUserExists(final String userName) {
	    ObjectNode queryNode = mapper.createObjectNode();
	    queryNode.put(USERNAME, userName);

        return DatabaseServiceImpl.getInstance().findOne(IDENTITY_STORE_DB, queryNode) != null ? true : false;
    }
}
