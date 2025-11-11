package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivitySessionStartedRepository extends MongoRepository<ActivitySessionStartedDocument, String> {
	Optional<ActivitySessionStartedDocument> findByEventId(String eventId);
}


