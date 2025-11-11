package com.flowlinker.events.projection.security;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SecurityLoginSuccessRepository extends MongoRepository<SecurityLoginSuccessDocument, String> {
	Optional<SecurityLoginSuccessDocument> findByEventId(String eventId);
}


