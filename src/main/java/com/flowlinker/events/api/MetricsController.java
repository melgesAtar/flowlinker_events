package com.flowlinker.events.api;

import com.flowlinker.events.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
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

	@GetMapping("/errors")
	public ResponseEntity<Map<String, Object>> errors(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours
	) {
		return ResponseEntity.ok(metricsService.errorSummary(MetricsService.DurationRange.lastHours(hours), customerId));
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

	@GetMapping("/shares/count")
	public ResponseEntity<Map<String, Long>> sharesCount(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours
	) {
		long value = metricsService.sharesCount(MetricsService.DurationRange.lastHours(hours), customerId);
		return ResponseEntity.ok(Map.of("shares", value));
	}

	@GetMapping("/direct-messages/count")
	public ResponseEntity<Map<String, Long>> directMessagesCount(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours
	) {
		long value = metricsService.directMessagesCount(MetricsService.DurationRange.lastHours(hours), customerId);
		return ResponseEntity.ok(Map.of("directMessages", value));
	}

	@GetMapping("/campaigns/count")
	public ResponseEntity<Map<String, Long>> campaignsCount(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "hours", defaultValue = "24") int hours
	) {
		long value = metricsService.campaignsCount(MetricsService.DurationRange.lastHours(hours), customerId);
		return ResponseEntity.ok(Map.of("campaigns", value));
	}

	@GetMapping("/accounts/last-device")
	public ResponseEntity<Map<String, Object>> lastDevice(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "account") String account,
		@RequestParam(name = "tz", defaultValue = "America/Sao_Paulo") String tz
	) {
		return metricsService.lastDeviceForAccount(customerId, account, tz)
			.map(map -> {
				Map<String, Object> body = new LinkedHashMap<>();
				body.put("lastUsedAt", map.get("lastUsedAt"));
				body.put("lastUsedAtLocal", map.get("lastUsedAtLocal"));
				return ResponseEntity.ok(body);
			})
			.orElseGet(() -> ResponseEntity.ok(Map.of("message", "Conta nunca utilizada")));
	}

	@GetMapping("/accounts/suspension-status")
	public ResponseEntity<Map<String, Object>> suspensionStatus(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "account") String account,
		@RequestParam(name = "tz", defaultValue = "America/Sao_Paulo") String tz
	) {
		return metricsService.accountSuspensionStatus(customerId, account, tz)
			.map(map -> {
				Map<String, Object> body = new LinkedHashMap<>();
				body.put("suspendedSince", map.get("suspendedSince"));
				body.put("suspendedSinceLocal", map.get("suspendedSinceLocal"));
				body.put("now", map.get("now"));
				body.put("nowLocal", map.get("nowLocal"));
				body.put("durationSeconds", map.get("durationSeconds"));
				body.put("durationMinutes", map.get("durationMinutes"));
				body.put("durationHours", map.get("durationHours"));
				body.put("reason", map.get("reason"));
				return ResponseEntity.ok(body);
			})
			.orElseGet(() -> ResponseEntity.ok(Map.of("message", "Conta não está suspensa ou nunca foi suspensa")));
	}

	@GetMapping("/accounts/block-status")
	public ResponseEntity<Map<String, Object>> blockStatus(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "account") String account,
		@RequestParam(name = "tz", defaultValue = "America/Sao_Paulo") String tz
	) {
		return metricsService.accountBlockStatus(customerId, account, tz)
			.map(map -> {
				Map<String, Object> body = new LinkedHashMap<>();
				body.put("blockedSince", map.get("blockedSince"));
				body.put("blockedSinceLocal", map.get("blockedSinceLocal"));
				body.put("now", map.get("now"));
				body.put("nowLocal", map.get("nowLocal"));
				body.put("durationSeconds", map.get("durationSeconds"));
				body.put("durationMinutes", map.get("durationMinutes"));
				body.put("durationHours", map.get("durationHours"));
				body.put("reason", map.get("reason"));
				return ResponseEntity.ok(body);
			})
			.orElseGet(() -> ResponseEntity.ok(Map.of("message", "Conta não está bloqueada ou nunca foi bloqueada")));
	}
}


