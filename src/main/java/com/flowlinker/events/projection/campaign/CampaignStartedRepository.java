package com.flowlinker.events.projection.campaign;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CampaignStartedRepository extends MongoRepository<CampaignStartedDocument, String> {
	Optional<CampaignStartedDocument> findByEventId(String eventId);
}


