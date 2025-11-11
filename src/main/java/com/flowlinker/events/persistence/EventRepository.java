package com.flowlinker.events.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EventRepository extends MongoRepository<EventDocument, String> {
	Optional<EventDocument> findByEventId(String eventId);
}


