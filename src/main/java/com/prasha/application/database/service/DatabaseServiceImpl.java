package com.prasha.application.database.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.*;
import com.prasha.application.exception.service.StreamServiceException;
import org.bson.Document;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.logging.Logger;

import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;

public class DatabaseServiceImpl implements DatabaseService {
    private static final String STREAM_SERVICE_DB = "streamServiceDb";
    private static final Logger log = Logger.getLogger(DatabaseServiceImpl.class.getName());
    private static final DatabaseServiceImpl DB_SERVICE_IMPL = new DatabaseServiceImpl();
    private static MongoClient mongoClient;
    private static MongoDatabase db;

    private DatabaseServiceImpl() {
        mongoClient = MongoClients.create();
        // Verify mongoclient shouldn't be null
        db = mongoClient.getDatabase(STREAM_SERVICE_DB);
    }

    public static DatabaseServiceImpl getInstance() {
        return DB_SERVICE_IMPL;
    }

    private static Document convertJSONtoBSON(JsonNode jsonNode) {
        Document document = Document.parse(jsonNode.toString());
        return document;
    }

    private static MongoCollection getCollection(final MongoDatabase db, String collectionName) {
        return db.getCollection(collectionName);
    }

    @Override
    public void insertOne(final String collectionName, final JsonNode jsonNode) {
        MongoCollection<Document> collection = getCollection(db, collectionName);
        collection.insertOne(convertJSONtoBSON(jsonNode));
    }

    @Override
    public JsonNode findOne(final String collectionName, final JsonNode query) {
        MongoCollection<Document> collection = getCollection(db, collectionName);
        try {
            FindIterable<Document> doc = collection.find(convertJSONtoBSON(query)).projection(fields(excludeId()));
            if (doc.first() == null) {
                return null;
            }

            final String result = doc.first().toJson();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode resultNode;
            resultNode = mapper.readTree(result);
            return resultNode;
        } catch (IOException e) {
            throw new StreamServiceException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
