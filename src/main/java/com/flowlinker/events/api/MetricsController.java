package com.flowlinker.events.api;

import com.flowlinker.events.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

	private final MetricsService metricsService;

	public MetricsController(MetricsService metricsService) {
		this.metricsService = metricsService;
	}

	@GetMapping("/overview")
	public ResponseEntity<Map<String, Object>> overview(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours
	) {
		return ResponseEntity.ok(metricsService.overview(MetricsService.DurationRange.lastHours(hours), customerId));
	}

	@GetMapping("/actions/summary")
	public ResponseEntity<Map<String, Long>> actionsSummary(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours
	) {
		return ResponseEntity.ok(metricsService.actionsSummary(MetricsService.DurationRange.lastHours(hours), customerId));
	}

	@GetMapping("/people-reached")
	public ResponseEntity<Map<String, Long>> peopleReached(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours
	) {
		long value = metricsService.peopleReachedApprox(MetricsService.DurationRange.lastHours(hours), customerId);
		return ResponseEntity.ok(Map.of("peopleReached", value));
	}

	@GetMapping("/recent")
	public ResponseEntity<List<Map<String, Object>>> recent(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "limit", defaultValue = "20") int limit,
		@RequestParam(name = "tz", defaultValue = "America/Sao_Paulo") String tz
	) {
		return ResponseEntity.ok(metricsService.recentActivities(customerId, limit, tz));
	}

	@GetMapping("/debug/account-created")
	public ResponseEntity<List<Map<String, Object>>> accountCreated(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "limit", defaultValue = "20") int limit,
		@RequestParam(name = "tz", defaultValue = "America/Sao_Paulo") String tz
	) {
		return ResponseEntity.ok(metricsService.listAccountCreated(customerId, limit, tz));
	}

	@GetMapping("/extractions/events")
	public ResponseEntity<List<Map<String, Object>>> extractionEvents(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "limit", defaultValue = "20") int limit,
		@RequestParam(name = "tz", defaultValue = "America/Sao_Paulo") String tz
	) {
		return ResponseEntity.ok(metricsService.extractionEvents(customerId, limit, tz));
	}
	@GetMapping("/distribution/social")
	public ResponseEntity<Map<String, Long>> distribution(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours
	) {
		return ResponseEntity.ok(metricsService.distributionByNetwork(MetricsService.DurationRange.lastHours(hours), customerId));
	}

	@GetMapping("/daily")
	public ResponseEntity<List<Map<String, Object>>> daily(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "days", defaultValue = "7") int days
	) {
		return ResponseEntity.ok(metricsService.dailyActions(customerId, days));
	}

	@GetMapping("/heatmap")
	public ResponseEntity<List<Map<String, Object>>> heatmap(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "days", defaultValue = "7") int days
	) {
		return ResponseEntity.ok(metricsService.heatmap(customerId, days));
	}

	@GetMapping("/ranking/personas")
	public ResponseEntity<List<Map<String, Object>>> ranking(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours,
		@RequestParam(name = "limit", defaultValue = "10") int limit
	) {
		return ResponseEntity.ok(metricsService.rankingPersonas(MetricsService.DurationRange.lastHours(hours), customerId, limit));
	}
}


