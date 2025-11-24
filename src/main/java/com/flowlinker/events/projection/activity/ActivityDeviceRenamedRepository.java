package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActivityDeviceRenamedRepository extends MongoRepository<ActivityDeviceRenamedDocument, String> {
	Optional<ActivityDeviceRenamedDocument> findByEventId(String eventId);
}



