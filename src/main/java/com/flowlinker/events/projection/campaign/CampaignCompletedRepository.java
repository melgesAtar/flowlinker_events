package com.flowlinker.events.projection.campaign;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CampaignCompletedRepository extends MongoRepository<CampaignCompletedDocument, String> {
	Optional<CampaignCompletedDocument> findByEventId(String eventId);
}


