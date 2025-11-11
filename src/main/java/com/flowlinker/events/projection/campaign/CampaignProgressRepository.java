package com.flowlinker.events.projection.campaign;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CampaignProgressRepository extends MongoRepository<CampaignProgressDocument, String> {
	Optional<CampaignProgressDocument> findByEventId(String eventId);
}


