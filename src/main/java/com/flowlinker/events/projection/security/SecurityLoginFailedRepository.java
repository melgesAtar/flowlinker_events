package com.flowlinker.events.projection.security;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SecurityLoginFailedRepository extends MongoRepository<SecurityLoginFailedDocument, String> {
	Optional<SecurityLoginFailedDocument> findByEventId(String eventId);
}


