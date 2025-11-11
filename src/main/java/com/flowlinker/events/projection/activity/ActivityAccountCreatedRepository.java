package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityAccountCreatedRepository extends MongoRepository<ActivityAccountCreatedDocument, String> {
	Optional<ActivityAccountCreatedDocument> findByEventId(String eventId);
}


