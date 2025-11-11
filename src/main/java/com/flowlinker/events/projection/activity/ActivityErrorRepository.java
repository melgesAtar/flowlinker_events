package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityErrorRepository extends MongoRepository<ActivityErrorDocument, String> {
	Optional<ActivityErrorDocument> findByEventId(String eventId);
}


