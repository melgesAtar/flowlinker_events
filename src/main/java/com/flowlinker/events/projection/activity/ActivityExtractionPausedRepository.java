package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityExtractionPausedRepository extends MongoRepository<ActivityExtractionPausedDocument, String> {
	Optional<ActivityExtractionPausedDocument> findByEventId(String eventId);
}


