package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityAccountSuspendedRepository extends MongoRepository<ActivityAccountSuspendedDocument, String> {
	Optional<ActivityAccountSuspendedDocument> findByEventId(String eventId);
}



