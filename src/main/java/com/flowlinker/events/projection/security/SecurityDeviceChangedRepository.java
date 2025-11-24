package com.flowlinker.events.projection.security;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SecurityDeviceChangedRepository extends MongoRepository<SecurityDeviceChangedDocument, String> {
	Optional<SecurityDeviceChangedDocument> findByEventId(String eventId);
}



