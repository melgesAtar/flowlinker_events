package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityAccountUpdatedRepository extends MongoRepository<ActivityAccountUpdatedDocument, String> {
	Optional<ActivityAccountUpdatedDocument> findByEventId(String eventId);
}


