package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityExtractionStartedRepository extends MongoRepository<ActivityExtractionStartedDocument, String> {
	Optional<ActivityExtractionStartedDocument> findByEventId(String eventId);
}


