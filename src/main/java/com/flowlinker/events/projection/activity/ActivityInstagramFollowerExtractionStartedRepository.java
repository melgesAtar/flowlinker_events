package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityInstagramFollowerExtractionStartedRepository extends MongoRepository<ActivityInstagramFollowerExtractionStartedDocument, String> {
	Optional<ActivityInstagramFollowerExtractionStartedDocument> findByEventId(String eventId);
}

