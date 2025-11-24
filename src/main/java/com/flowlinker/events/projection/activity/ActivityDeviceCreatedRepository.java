package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityDeviceCreatedRepository extends MongoRepository<ActivityDeviceCreatedDocument, String> {
	Optional<ActivityDeviceCreatedDocument> findByEventId(String eventId);
}



