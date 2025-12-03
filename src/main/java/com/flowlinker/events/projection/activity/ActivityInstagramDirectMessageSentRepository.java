package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityInstagramDirectMessageSentRepository extends MongoRepository<ActivityInstagramDirectMessageSentDocument, String> {
	Optional<ActivityInstagramDirectMessageSentDocument> findByEventId(String eventId);
}

