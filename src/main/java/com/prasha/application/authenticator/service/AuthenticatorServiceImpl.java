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

import static com.prasha.application.CommonUtils.*;

public class AuthenticatorServiceImpl implements AuthenticatorService {

    private static final Logger log = Logger.getLogger(AuthenticatorServiceImpl.class.getName());
    private static final AuthenticatorService AUTH_SERVICE = new AuthenticatorServiceImpl();
    private static final DatabaseServiceImpl dbService = DatabaseServiceImpl.getInstance();
    // TODO: there might be affects of declaring mapper as static check once
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String secretKey = "JYreE_cMH6U5OaByLOg3nSKUCwk3b0lYexyhQhLUcb5iuSBM5H6l5LDVux_epBV9-Y8qbRG_QmOF-iETzs7lhmEp_A6vRGsF5IB5uJkao_h-RBB1NJqRd5e6XWuk9CS6jiSzN8eIfLJCI_Mn2xZOE9p_5U_Wmqqsajo2G7odSudAjn_6L3q94e_TOKqaeevfMhbJms9kSSzPlu1qszxG2y5naXclybLDIG51-U7dup26kbwtdK0SxUfu6NHCfdkdl_xrfuikbq5QaZjTO6M0JUAl1MU3CdVsyMHM3uUIMF33wwwzU1dCeJ1Ld2zThSFMJcAtm8wVnmyn-vhW4BEifw";
    // 30 days in seconds
    private static Long expiryInSecs = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(2592000L).toEpochSecond(ZoneOffset.UTC);

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

    public static AuthenticatorService getInstance() {
        return AUTH_SERVICE;
    }

    @Override
	public JsonNode generateUserAccessToken(String requestCredential) {
        JsonNode result = verifyUserCredentials(requestCredential);
        if (result == null) {
            throw new StreamServiceException(Response.Status.FORBIDDEN);
        }

        ObjectNode jNode = mapper.createObjectNode();
        String jwt = createJWTToken(APP_NAME, result.get(USERNAME).asText());
        validateToken(jwt);
        // Insert access token corresponding to user in db with expiry time
        return jNode.put(ACCESS_TOKEN, jwt);
	}

	@Override
	public void createUser(JsonNode requestJson) {
        JsonNode result = getUserData(requestJson.get(USERNAME).asText());
        if (result != null) {
            throw new StreamServiceException(Response.Status.CONFLICT);
        }

        dbService.insertOne(IDENTITY_STORE_DB, requestJson);
    }

	/*TODO: Can change from random UUID to random base64 string?*/
	private String createJWTToken(String appName, String userName) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
	    // secretKey being used for generating token
	    byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(this.secretKey);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        String token = Jwts.builder().setIssuer(appName)
                .setSubject(userName)
                .setExpiration(Date.from(Instant.ofEpochSecond(expiryInSecs)
                        .atOffset(ZoneOffset.UTC).toInstant()))
                .setIssuedAt(Date.from(LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC)))
                .claim(USERNAME, userName)
                .signWith(SignatureAlgorithm.HS256, signingKey)
                .compact();

        return token;
    }

    public String validateToken(String jwt) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
                    .parseClaimsJws(jwt).getBody();
            String userName = (String) claims.get(USERNAME);
            return userName;
        } catch (ExpiredJwtException | SignatureException | MalformedJwtException e) {
            throw new StreamServiceException(Response.Status.FORBIDDEN);
        }
    }

    private String generateRandomKey() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private String decoder(String credential) {
        return new String(Base64.getDecoder().decode(credential));
    }

    private JsonNode verifyUserCredentials(String requestCredential) {
        String decodedCredential = decoder(requestCredential);
        String userName = decodedCredential.split(":")[0];
        String password = decodedCredential.split(":")[1];

        if (getUserData(userName) == null) {
            throw new StreamServiceException(Response.Status.NOT_FOUND);
        }

        ArrayNode array = mapper.createArrayNode();
        ObjectNode queryNode = mapper.createObjectNode();
        array.add(mapper.createObjectNode().put(USERNAME, userName));
        array.add(mapper.createObjectNode().put(PASSWORD, password));
        queryNode.put(DatabaseService.MONGO_AND_OPERATOR, array);

        JsonNode result = DatabaseServiceImpl.getInstance().findOne(IDENTITY_STORE_DB, queryNode);
        return result != null ? result : null;
    }

    private JsonNode getUserData(final String userName) {
	    ObjectNode queryNode = mapper.createObjectNode();
	    queryNode.put(USERNAME, userName);

        JsonNode result = DatabaseServiceImpl.getInstance().findOne(IDENTITY_STORE_DB, queryNode);
        return result != null ? result : null;
    }
}
