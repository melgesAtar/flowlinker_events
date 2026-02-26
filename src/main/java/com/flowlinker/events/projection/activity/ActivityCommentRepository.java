package com.flowlinker.events.projection.activity;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityCommentRepository extends MongoRepository<ActivityCommentDocument, String> {
}

