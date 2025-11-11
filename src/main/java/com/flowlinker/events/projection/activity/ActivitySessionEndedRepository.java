package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivitySessionEndedRepository extends MongoRepository<ActivitySessionEndedDocument, String> {
	Optional<ActivitySessionEndedDocument> findByEventId(String eventId);
}


