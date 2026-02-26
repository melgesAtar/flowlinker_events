package com.flowlinker.events.api;

import com.flowlinker.events.service.MetricsService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
@PreAuthorize("hasRole('METRICS')")
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

	@GetMapping("/campaigns/details")
	public ResponseEntity<?> campaignsDetails(
		@RequestParam(name = "customerId") String customerId,
		@RequestParam(name = "start") String start,
		@RequestParam(name = "end") String end,
		@RequestParam(name = "tz", defaultValue = "UTC") String tz,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "20") int size,
		@RequestParam(name = "format", defaultValue = "json") String format
	) {
		ZoneId zone = safeZone(tz);
		try {
			if (page < 0) return ResponseEntity.badRequest().body(Map.of("message", "page must be >= 0"));
			if (size <= 0 || size > 1000) return ResponseEntity.badRequest().body(Map.of("message", "size must be between 1 and 1000"));

			Instant from = parseStart(start, zone);
			Instant to = parseEnd(end, zone);
			// valida e cria o DurationRange (pode lançar IllegalArgumentException)
			MetricsService.DurationRange range = MetricsService.DurationRange.between(from, to);
			List<Map<String, Object>> all = metricsService.listCampaignsWithActivities(range, customerId, zone.getId());

			if ("xlsx".equalsIgnoreCase(format)) {
				// gera planilha completa (sem paginação)
				byte[] bytes = buildCampaignsXlsx(all);
				HttpHeaders headers = new HttpHeaders();
				headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=campaigns_details.xlsx");
				headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
				return ResponseEntity.ok().headers(headers).body(bytes);
			}

			int total = all.size();
			int fromIndex = page * size;
			if (fromIndex >= total) {
				Map<String, Object> body = new LinkedHashMap<>();
				body.put("total", total);
				body.put("page", page);
				body.put("size", size);
				body.put("items", List.of());
				return ResponseEntity.ok(body);
			}
			int toIndex = Math.min(fromIndex + size, total);
			List<Map<String, Object>> items = all.subList(fromIndex, toIndex);

			Map<String, Object> body = new LinkedHashMap<>();
			body.put("total", total);
			body.put("page", page);
			body.put("size", size);
			body.put("items", items);
			return ResponseEntity.ok(body);
		} catch (DateTimeParseException | IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	private byte[] buildCampaignsXlsx(List<Map<String, Object>> campaigns) {
		try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sCampaigns = wb.createSheet("campaigns");
			Sheet sMessages = wb.createSheet("directMessages");
			Sheet sShares = wb.createSheet("shares");

			// header campaigns
			Row h = sCampaigns.createRow(0);
			String[] ch = new String[] {"campaignId", "campaignName", "platform", "campaignType", "startedAt", "startedAtLocal", "lastProgressAt", "lastProgressAtLocal", "completedAt", "completedAtLocal", "total", "directMessagesCount", "sharesCount"};
			for (int i = 0; i < ch.length; i++) { Cell c = h.createCell(i); c.setCellValue(ch[i]); }

			int rowIdx = 1;
			int msgRow = 1; // start from 1 because 0 is header
			int shareRow = 1;
			// headers messages
			Row mh = sMessages.createRow(0);
			String[] mhc = new String[] {"campaignId", "eventId", "eventAt", "eventAtLocal", "account", "recipientUsername", "recipientId", "messagePreview", "source", "deviceId", "ip"};
			for (int i = 0; i < mhc.length; i++) { mh.createCell(i).setCellValue(mhc[i]); }
			// headers shares
			Row sh = sShares.createRow(0);
			String[] shc = new String[] {"campaignId", "eventId", "eventAt", "eventAtLocal", "account", "groupName", "groupUrl", "groupMembers", "post"};
			for (int i = 0; i < shc.length; i++) { sh.createCell(i).setCellValue(shc[i]); }

			for (Map<String, Object> c : campaigns) {
				Row r = sCampaigns.createRow(rowIdx++);
				int ci = 0;
				createCell(r, ci++, c.get("campaignId"));
				createCell(r, ci++, c.get("campaignName"));
				createCell(r, ci++, c.get("platform"));
				createCell(r, ci++, c.get("campaignType"));
				createCell(r, ci++, c.get("startedAt"));
				createCell(r, ci++, c.get("startedAtLocal"));
				createCell(r, ci++, c.get("lastProgressAt"));
				createCell(r, ci++, c.get("lastProgressAtLocal"));
				createCell(r, ci++, c.get("completedAt"));
				createCell(r, ci++, c.get("completedAtLocal"));
				createCell(r, ci++, c.get("total"));
				createCell(r, ci++, c.get("directMessagesCount"));
				createCell(r, ci++, c.get("sharesCount"));

				// direct messages
				Object dmObj = c.get("directMessages");
				if (dmObj instanceof List) {
					List<?> dms = (List<?>) dmObj;
					for (Object o : dms) {
						if (!(o instanceof Map)) continue;
						Map<?, ?> m = (Map<?, ?>) o;
						Row mr = sMessages.createRow(msgRow++);
						int mi = 0;
						createCell(mr, mi++, c.get("campaignId"));
						createCell(mr, mi++, m.get("eventId"));
						createCell(mr, mi++, m.get("eventAt"));
						createCell(mr, mi++, m.get("eventAtLocal"));
						createCell(mr, mi++, m.get("account"));
						createCell(mr, mi++, m.get("recipientUsername"));
						createCell(mr, mi++, m.get("recipientId"));
						createCell(mr, mi++, m.get("messagePreview"));
						createCell(mr, mi++, m.get("source"));
						createCell(mr, mi++, m.get("deviceId"));
						createCell(mr, mi++, m.get("ip"));
					}
				}

				// shares
				Object shObj = c.get("shares");
				if (shObj instanceof List) {
					List<?> shs = (List<?>) shObj;
					for (Object o : shs) {
						if (!(o instanceof Map)) continue;
						Map<?, ?> s = (Map<?, ?>) o;
						Row sr = sShares.createRow(shareRow++);
						int si = 0;
						createCell(sr, si++, c.get("campaignId"));
						createCell(sr, si++, s.get("eventId"));
						createCell(sr, si++, s.get("eventAt"));
						createCell(sr, si++, s.get("eventAtLocal"));
						createCell(sr, si++, s.get("account"));
						createCell(sr, si++, s.get("groupName"));
						createCell(sr, si++, s.get("groupUrl"));
						createCell(sr, si++, s.get("groupMembers"));
						createCell(sr, si++, s.get("post"));
					}
				}
			}

			// autosize columns for sheets (limit columns to a reasonable number)
			for (int i = 0; i < ch.length; i++) sCampaigns.autoSizeColumn(i);
			for (int i = 0; i < mhc.length; i++) sMessages.autoSizeColumn(i);
			for (int i = 0; i < shc.length; i++) sShares.autoSizeColumn(i);

			wb.write(out);
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to build XLSX", e);
		}
	}

	private void createCell(Row r, int idx, Object v) {
		Cell c = r.createCell(idx);
		if (v == null) { c.setBlank(); return; }
		if (v instanceof Number) {
			c.setCellValue(((Number) v).doubleValue());
			return;
		}
		if (v instanceof Instant) {
			c.setCellValue(v.toString());
			return;
		}
		c.setCellValue(String.valueOf(v));
	}

	private Instant parseStart(String s, ZoneId zone) {
		try {
			// tenta ISO_INSTANT
			return Instant.parse(s);
		} catch (DateTimeParseException e) {
			// tenta yyyy-MM-dd como inicio do dia
			LocalDate d = LocalDate.parse(s);
			return d.atStartOfDay(zone).toInstant();
		}
	}

	private Instant parseEnd(String s, ZoneId zone) {
		try {
			return Instant.parse(s);
		} catch (DateTimeParseException e) {
			LocalDate d = LocalDate.parse(s);
			// fim do dia
			return d.atTime(LocalTime.MAX).atZone(zone).toInstant();
		}
	}

	private ZoneId safeZone(String zoneId) {
		try {
			return ZoneId.of(zoneId);
		} catch (Exception e) {
			return ZoneId.of("UTC");
		}
	}
}
