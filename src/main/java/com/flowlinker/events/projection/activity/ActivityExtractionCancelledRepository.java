package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityExtractionCancelledRepository extends MongoRepository<ActivityExtractionCancelledDocument, String> {
	Optional<ActivityExtractionCancelledDocument> findByEventId(String eventId);
}


