package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityExtractionCompletedRepository extends MongoRepository<ActivityExtractionCompletedDocument, String> {
	Optional<ActivityExtractionCompletedDocument> findByEventId(String eventId);
}


