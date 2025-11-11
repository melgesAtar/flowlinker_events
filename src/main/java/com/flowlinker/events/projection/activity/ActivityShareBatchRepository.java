package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityShareBatchRepository extends MongoRepository<ActivityShareBatchDocument, String> {
	Optional<ActivityShareBatchDocument> findByEventId(String eventId);
}


