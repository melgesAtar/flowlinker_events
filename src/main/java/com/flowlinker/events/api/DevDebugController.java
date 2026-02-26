package com.flowlinker.events.api;

import com.flowlinker.events.projection.activity.ActivityCommentDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/debug")
public class DevDebugController {

    private final MongoTemplate mongoTemplate;

    public DevDebugController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/activity-comments")
    public ResponseEntity<List<Map<String, Object>>> activityComments(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        if (limit <= 0) limit = 20;
        Query q = Query.query(Criteria.where("_class").is("com.flowlinker.events.projection.activity.ActivityCommentDocument"))
                .with(Sort.by(Sort.Direction.DESC, "eventAt"))
                .limit(limit);
        List<ActivityCommentDocument> docs = mongoTemplate.find(q, ActivityCommentDocument.class);
        List<Map<String,Object>> items = docs.stream().map(d -> {
            java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("eventId", d.getEventId());
            m.put("eventAt", d.getEventAt());
            m.put("receivedAt", d.getReceivedAt());
            m.put("customerId", d.getCustomerId());
            m.put("deviceId", d.getDeviceId());
            m.put("ip", d.getIp());
            m.put("platform", d.getPlatform());
            m.put("account", d.getAccount());
            m.put("campaignId", d.getCampaignId());
            m.put("campaignName", d.getCampaignName());
            m.put("success", d.getSuccess());
            m.put("orderIndex", d.getOrderIndex());
            m.put("commentText", d.getCommentText());
            m.put("errorReason", d.getErrorReason());
            m.put("errorMessage", d.getErrorMessage());
            m.put("retryable", d.getRetryable());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }
}
