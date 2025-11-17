package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityAccountBlockedRepository extends MongoRepository<ActivityAccountBlockedDocument, String> {
	Optional<ActivityAccountBlockedDocument> findByEventId(String eventId);
}



