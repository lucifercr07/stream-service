package com.prasha.application.database.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface DatabaseService {
	String MONGO_AND_OPERATOR = "$and";

    void insertOne(final String collectionName, final JsonNode jsonNode);
	JsonNode findOne(final String collectionName, final JsonNode query);
}
