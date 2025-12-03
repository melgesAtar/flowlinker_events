package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivitySocialMediaAccountSuspendedRepository extends MongoRepository<ActivitySocialMediaAccountSuspendedDocument, String> {
	Optional<ActivitySocialMediaAccountSuspendedDocument> findByEventId(String eventId);
}

