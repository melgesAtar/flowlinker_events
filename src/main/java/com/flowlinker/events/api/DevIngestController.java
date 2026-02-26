package com.flowlinker.events.api;

import com.flowlinker.events.api.dto.EnrichedEventDTO;
import com.flowlinker.events.consumer.EventConsumerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class DevIngestController {

    private final EventConsumerService consumerService;

    public DevIngestController(EventConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestBody EnrichedEventDTO event) {
        // chama publicamente o handler que o Rabbit invoca
        consumerService.onActivity(event);
        return ResponseEntity.ok().build();
    }
}

