package com.flowlinker.events.service;

import com.flowlinker.events.projection.activity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetricsService {

	private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

	private final MongoTemplate mongoTemplate;

	public MetricsService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public Map<String, Object> overview(DurationRange range, String customerId) {
		Instant from = range.from();
		Instant to = range.to();
		long actions = countShareBatch(customerId, from, to);
		long errors = countActivityErrors(customerId, from, to);
		double errorRate = (actions + errors) == 0 ? 0.0 : (errors * 100.0) / (actions + errors);
		long peopleReached = peopleReachedApprox(range, customerId);
		long activePersonas = countActivePersonas(customerId, from, to);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("totalActions", actions);
		map.put("peopleReached", peopleReached);
		map.put("activePersonas", activePersonas);
		map.put("errorRate", errorRate);
		return map;
	}

	public Map<String, Long> actionsSummary(DurationRange range, String customerId) {
		Instant from = range.from();
		Instant to = range.to();
		long shares = 0;
		long extractions = 0;
		try {
			shares = countShareBatch(customerId, from, to);
		} catch (DataAccessException e) {
			log.warn("Falha ao contar shares", e);
		}
		try {
			extractions = mongoTemplate.count(timeRangeCustomer(customerId, from, to), com.flowlinker.events.projection.campaign.CampaignStartedDocument.class);
		} catch (DataAccessException e) {
			log.warn("Falha ao contar extractions", e);
		}
		long instagramLikes = 0;
		long instagramComments = 0;
		long total = shares + extractions + instagramLikes + instagramComments;
		Map<String, Long> out = new LinkedHashMap<>();
		out.put("shares", shares);
		out.put("extractions", extractions);
		out.put("instagramLikes", instagramLikes);
		out.put("instagramComments", instagramComments);
		out.put("total", total);
		return out;
	}

	public List<Map<String, Object>> recentActivities(String customerId, int limit, String zoneId) {
		ZoneId zone = safeZone(zoneId);
		Query q = Query.query(Criteria.where("customerId").is(customerId))
			.with(Sort.by(Sort.Direction.DESC, "eventAt")).limit(limit);
		List<com.flowlinker.events.persistence.EventDocument> events =
			mongoTemplate.find(q, com.flowlinker.events.persistence.EventDocument.class);

		List<Map<String, Object>> items = new ArrayList<>(events.size());
		for (com.flowlinker.events.persistence.EventDocument ev : events) {
			String type = ev.getEventType();
			Map<String, Object> payload = ev.getPayload() == null ? Collections.emptyMap() : ev.getPayload();
			String actor = s(payload.get("account"));

			// Se o desktop já mandou um resumo humano, priorize
			String text = s(payload.get("text"));

			if (text == null || text.isBlank()) {
			// segurança (suporta convenções com ponto e com underline)
			if ("desktop.security.login_success".equals(type) || "desktop.security.login.success".equals(type)) {
				text = "fez login com sucesso";
			} else if ("desktop.security.login_failed".equals(type) || "desktop.security.login.failed".equals(type)) {
				String reason = s(payload.get("reason"));
				text = "falha de login" + (reason == null || reason.isBlank() ? "" : ": " + reason.toLowerCase(Locale.ROOT).replace('_', ' '));
			} else if ("desktop.activity.session_started".equals(type) || "desktop.activity.session.started".equals(type)) {
				text = "iniciou nova sessão";
			} else if ("desktop.activity.session_ended".equals(type) || "desktop.activity.session.ended".equals(type) || "facebook.activity.session_ended".equals(type)) {
				String reason = s(payload.get("reason"));
				text = "encerrou sessão" + (reason == null || reason.isBlank() ? "" : " (" + reason + ")");
			} else if ("facebook.activity.share_batch".equals(type) || "facebook.activity.share.batch".equals(type)) {
				Integer count = i(payload.get("count"));
				String platform = s(payload.get("platform"));
				String groupName = s(payload.get("groupName"));
				text = "compartilhou " + (count == null ? 1 : count) + " publicação"
					+ ((count != null && count > 1) ? "s" : "")
					+ " no " + toLower(platform == null ? "FACEBOOK" : platform)
					+ (groupName == null || groupName.isBlank() ? "" : " (grupo: " + groupName + ")");
			} else if ("facebook.activity.error".equals(type) || "desktop.activity.error".equals(type)) {
				text = "Falha: " + nullToEmpty(s(payload.get("message")));
			} else if ("facebook.activity.account_created".equals(type)) {
				String profileName = s(payload.get("profileName"));
				text = "criou conta/perfil" + (profileName == null || profileName.isBlank() ? "" : " (" + profileName + ")");
			} else if ("facebook.activity.account_updated".equals(type)) {
				String profileName = s(payload.get("profileName"));
				text = "atualizou conta/perfil" + (profileName == null || profileName.isBlank() ? "" : " (" + profileName + ")");
			// extração (novos eventos)
			} else if ("facebook.activity.extraction.started".equals(type)) {
				String keywords = s(payload.get("keywords"));
				String platform = s(payload.get("platform"));
				text = "Extração Iniciada - " + (keywords == null ? "" : "Keywords: " + keywords)
					+ (platform == null ? "" : " (" + platform + ")");
			} else if ("facebook.activity.extraction.paused".equals(type)) {
				Long totalGroups = payload.get("totalGroups") instanceof Number ? ((Number) payload.get("totalGroups")).longValue() : 0L;
				Long totalMembers = payload.get("totalMembers") instanceof Number ? ((Number) payload.get("totalMembers")).longValue() : 0L;
				String keywords = s(payload.get("keywords"));
				text = String.format("Extração Pausada - %d grupos, %d pessoas%s",
					totalGroups, totalMembers, (keywords == null || keywords.isBlank() ? "" : " (keywords: " + keywords + ")"));
			} else if ("facebook.activity.extraction.cancelled".equals(type)) {
				Long totalGroups = payload.get("totalGroups") instanceof Number ? ((Number) payload.get("totalGroups")).longValue() : 0L;
				String keywords = s(payload.get("keywords"));
				text = String.format("Extração Cancelada - %d grupos processados%s",
					totalGroups, (keywords == null || keywords.isBlank() ? "" : " (keywords: " + keywords + ")"));
			} else if ("facebook.activity.extraction.completed".equals(type)) {
				Long totalGroups = payload.get("totalGroups") instanceof Number ? ((Number) payload.get("totalGroups")).longValue() : 0L;
				Long totalMembers = payload.get("totalMembers") instanceof Number ? ((Number) payload.get("totalMembers")).longValue() : 0L;
				String keywords = s(payload.get("keywords"));
				text = String.format("Extração Finalizada - %d grupos, %d pessoas%s",
					totalGroups, totalMembers, (keywords == null || keywords.isBlank() ? "" : " (keywords: " + keywords + ")"));
			} else if ("facebook.campaign.started".equals(type)) {
				text = "Campanha iniciada";
			} else if ("facebook.campaign.progress".equals(type)) {
				text = "Campanha em progresso";
			} else if ("facebook.campaign.completed".equals(type)) {
				text = "Campanha finalizada";
			}
			}

			Map<String, Object> item = new LinkedHashMap<>();
			item.put("eventAt", ev.getEventAt());
			item.put("eventAtLocal", ev.getEventAt().atZone(zone).toString());
			item.put("zone", zone.getId());
			item.put("type", type);
			if (actor != null) item.put("actor", actor);
			if (text != null) item.put("text", text);

			items.add(item);
		}
		return items;
	}

	private <T> Optional<T> findLatest(String customerId, Class<T> type) {
		try {
			Query q = Query.query(Criteria.where("customerId").is(customerId))
				.with(Sort.by(Sort.Direction.DESC, "eventAt")).limit(1);
			return Optional.ofNullable(mongoTemplate.findOne(q, type));
		} catch (DataAccessException e) {
			log.warn("Falha ao consultar mais recente de {}", type.getSimpleName(), e);
			return Optional.empty();
		}
	}

	public List<Map<String, Object>> listAccountCreated(String customerId, int limit, String zoneId) {
		ZoneId zone = safeZone(zoneId);
		List<Map<String, Object>> items = new ArrayList<>();
		try {
			Query qs = Query.query(Criteria.where("customerId").is(customerId))
				.with(Sort.by(Sort.Direction.DESC, "eventAt")).limit(limit);
			for (ActivityAccountCreatedDocument d : mongoTemplate.find(qs, ActivityAccountCreatedDocument.class)) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("eventId", d.getEventId());
				m.put("eventAt", d.getEventAt());
				m.put("eventAtLocal", d.getEventAt().atZone(zone).toString());
				m.put("zone", zone.getId());
				m.put("customerId", d.getCustomerId());
				m.put("deviceId", d.getDeviceId());
				m.put("ip", d.getIp());
				m.put("platform", d.getPlatform());
				m.put("account", d.getAccount());
				m.put("profileName", d.getProfileName());
				items.add(m);
			}
		} catch (DataAccessException e) {
			log.warn("Falha ao consultar account_created", e);
		}
		return items;
	}

	public List<Map<String, Object>> extractionEvents(String customerId, int limit, String zoneId) {
		ZoneId zone = safeZone(zoneId);
		List<Map<String, Object>> items = new ArrayList<>();
		try {
			Query base = Query.query(Criteria.where("customerId").is(customerId))
				.with(Sort.by(Sort.Direction.DESC, "eventAt")).limit(limit);

			for (ActivityExtractionStartedDocument d : mongoTemplate.find(base, ActivityExtractionStartedDocument.class)) {
				String keywords = nullToEmpty(d.getKeywords());
				String text = "Extração Iniciada - Keywords: " + keywords;
				items.add(extractionItem("started", d.getEventAt(), zone, text, safeLong(d.getTotalGroups()), safeLong(d.getTotalMembers())));
			}
			for (ActivityExtractionPausedDocument d : mongoTemplate.find(base, ActivityExtractionPausedDocument.class)) {
				String keywords = nullToEmpty(d.getKeywords());
				String text = String.format("Extração Pausada - %d grupos, %d pessoas (keywords: %s)", safeLong(d.getTotalGroups()), safeLong(d.getTotalMembers()), keywords);
				items.add(extractionItem("paused", d.getEventAt(), zone, text, safeLong(d.getTotalGroups()), safeLong(d.getTotalMembers())));
			}
			for (ActivityExtractionCancelledDocument d : mongoTemplate.find(base, ActivityExtractionCancelledDocument.class)) {
				String keywords = nullToEmpty(d.getKeywords());
				String text = String.format("Extração Cancelada - %d grupos processados (keywords: %s)", safeLong(d.getTotalGroups()), keywords);
				items.add(extractionItem("cancelled", d.getEventAt(), zone, text, safeLong(d.getTotalGroups()), safeLong(d.getTotalMembers())));
			}
			for (ActivityExtractionCompletedDocument d : mongoTemplate.find(base, ActivityExtractionCompletedDocument.class)) {
				String keywords = nullToEmpty(d.getKeywords());
				String text = String.format("Extração Finalizada - %d grupos, %d pessoas (keywords: %s)", safeLong(d.getTotalGroups()), safeLong(d.getTotalMembers()), keywords);
				items.add(extractionItem("completed", d.getEventAt(), zone, text, safeLong(d.getTotalGroups()), safeLong(d.getTotalMembers())));
			}
		} catch (DataAccessException e) {
			log.warn("Falha ao consultar eventos de extração", e);
		}
		return items.stream()
			.sorted(Comparator.comparing((Map<String, Object> m) -> (Instant) m.get("eventAt")).reversed())
			.limit(limit)
			.collect(Collectors.toList());
	}

	public Map<String, Long> distributionByNetwork(DurationRange range, String customerId) {
		Instant from = range.from();
		Instant to = range.to();
		Query q = timeRangeCustomer(customerId, from, to);
		List<ActivityShareBatchDocument> list = mongoTemplate.find(q, ActivityShareBatchDocument.class);
		return list.stream().collect(Collectors.groupingBy(
			d -> Optional.ofNullable(d.getPlatform()).orElse("DESCONHECIDO"),
			Collectors.counting()
		));
	}

	public List<Map<String, Object>> dailyActions(String customerId, int days) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
		ZonedDateTime start = now.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0);
		Query q = Query.query(Criteria.where("customerId").is(customerId)
			.and("eventAt").gte(start.toInstant()).lte(now.toInstant()));
		List<ActivityShareBatchDocument> list = mongoTemplate.find(q, ActivityShareBatchDocument.class);
		Map<String, Long> sharesPerDay = list.stream().collect(Collectors.groupingBy(
			d -> formatDay(d.getEventAt(), now.getZone()),
			TreeMap::new,
			Collectors.counting()
		));
		List<Map<String, Object>> out = new ArrayList<>();
		for (int i = 0; i < days; i++) {
			ZonedDateTime day = start.plusDays(i);
			String key = day.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("date", key);
			row.put("likes", 0);
			row.put("comments", 0);
			row.put("shares", sharesPerDay.getOrDefault(key, 0L));
			out.add(row);
		}
		return out;
	}

	public List<Map<String, Object>> heatmap(String customerId, int days) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
		ZonedDateTime start = now.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0);
		Query q = Query.query(Criteria.where("customerId").is(customerId)
			.and("eventAt").gte(start.toInstant()).lte(now.toInstant()));
		List<ActivityShareBatchDocument> list = mongoTemplate.find(q, ActivityShareBatchDocument.class);
		int[][] matrix = new int[7][24];
		for (ActivityShareBatchDocument d : list) {
			ZonedDateTime z = d.getEventAt().atZone(now.getZone());
			int dow = z.getDayOfWeek().getValue() % 7;
			int h = z.getHour();
			matrix[dow][h] += 1;
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (int dow = 0; dow < 7; dow++) {
			for (int h = 0; h < 24; h++) {
				Map<String, Object> cell = new LinkedHashMap<>();
				cell.put("dayOfWeek", dow);
				cell.put("hour", h);
				cell.put("count", matrix[dow][h]);
				out.add(cell);
			}
		}
		return out;
	}

	public List<Map<String, Object>> rankingPersonas(DurationRange range, String customerId, int limit) {
		Instant from = range.from();
		Instant to = range.to();
		Query q = timeRangeCustomer(customerId, from, to);
		List<ActivityShareBatchDocument> list = mongoTemplate.find(q, ActivityShareBatchDocument.class);
		Map<String, Long> counts = list.stream().collect(Collectors.groupingBy(
			d -> Optional.ofNullable(d.getAccount()).orElse("desconhecido"),
			Collectors.counting()
		));
		return counts.entrySet().stream()
			.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
			.limit(limit)
			.map(e -> {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("account", e.getKey());
				m.put("actions", e.getValue());
				return m;
			})
			.collect(Collectors.toList());
	}

	private long countShareBatch(String customerId, Instant from, Instant to) {
		try {
			return mongoTemplate.count(timeRangeCustomer(customerId, from, to), ActivityShareBatchDocument.class);
		} catch (DataAccessException e) {
			log.warn("Falha ao contar ActivityShareBatch", e);
			return 0;
		}
	}

	private long countActivityErrors(String customerId, Instant from, Instant to) {
		try {
			return mongoTemplate.count(timeRangeCustomer(customerId, from, to), ActivityErrorDocument.class);
		} catch (DataAccessException e) {
			log.warn("Falha ao contar ActivityError", e);
			return 0;
		}
	}

	public long peopleReachedApprox(DurationRange range, String customerId) {
		Instant from = range.from();
		Instant to = range.to();

		// 1) Projeção tipada (quando existir)
		List<ActivityShareBatchDocument> projected = Collections.emptyList();
		try {
			Query q = timeRangeCustomer(customerId, from, to).with(Sort.by(Sort.Direction.DESC, "eventAt"));
			projected = mongoTemplate.find(q, ActivityShareBatchDocument.class);
		} catch (DataAccessException e) {
			log.warn("Falha ao calcular peopleReached (projeção)", e);
		}

		// 2) Fallback: eventos brutos na coleção 'events' (aceita os dois formatos de tipo)
		List<com.flowlinker.events.persistence.EventDocument> raw = Collections.emptyList();
		try {
			Query qr = Query.query(Criteria.where("customerId").is(customerId)
				.and("eventAt").gte(from).lte(to)
				.and("eventType").in("facebook.activity.share_batch", "facebook.activity.share.batch"))
				.with(Sort.by(Sort.Direction.DESC, "eventAt"));
			raw = mongoTemplate.find(qr, com.flowlinker.events.persistence.EventDocument.class);
		} catch (DataAccessException e) {
			log.warn("Falha ao calcular peopleReached (bruto)", e);
		}

		// Deduplicação por grupo (usa o mais recente por grupo)
		Map<String, Long> membersByGroup = new HashMap<>();

		for (ActivityShareBatchDocument d : projected) {
			String key = Optional.ofNullable(d.getGroupUrl()).filter(s -> !s.isBlank())
				.orElse(Optional.ofNullable(d.getGroupName()).orElse(""));
			if (key.isBlank()) continue;
			membersByGroup.putIfAbsent(key, parseLongSafe(d.getGroupMembers()));
		}

		for (com.flowlinker.events.persistence.EventDocument ev : raw) {
			Map<String, Object> p = ev.getPayload();
			if (p == null) continue;
			String groupUrl = s(p.get("groupUrl"));
			String groupName = s(p.get("groupName"));
			String key = (groupUrl != null && !groupUrl.isBlank()) ? groupUrl : (groupName == null ? "" : groupName);
			if (key.isBlank()) continue;
			Long members = asLong(p.get("groupMembers"));
			if (members == null) continue;
			membersByGroup.putIfAbsent(key, members);
		}

		long sum = 0L;
		for (Long members : membersByGroup.values()) {
			long reached = Math.round(members * 0.30d); // heurística
			sum += reached;
		}
		return sum;
	}

	private long countActivePersonas(String customerId, Instant from, Instant to) {
		Query q = timeRangeCustomer(customerId, from, to);
		List<ActivitySessionStartedDocument> started = mongoTemplate.find(q, ActivitySessionStartedDocument.class);
		List<ActivityShareBatchDocument> shares = mongoTemplate.find(q, ActivityShareBatchDocument.class);
		Set<String> accounts = new HashSet<>();
		started.forEach(s -> { if (s.getAccount() != null) accounts.add(s.getAccount()); });
		shares.forEach(s -> { if (s.getAccount() != null) accounts.add(s.getAccount()); });
		return accounts.size();
	}

	private Query timeRangeCustomer(String customerId, Instant from, Instant to) {
		return Query.query(Criteria.where("customerId").is(customerId).and("eventAt").gte(from).lte(to));
	}

	private Map<String, Object> activityItem(Instant ts, ZoneId zone, String actor, String text) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("eventAt", ts);
		m.put("eventAtLocal", ts.atZone(zone).toString());
		m.put("zone", zone.getId());
		m.put("actor", actor);
		m.put("text", text);
		return m;
	}

	private Map<String, Object> extractionItem(String type, Instant ts, ZoneId zone, String text, long total, long members) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("type", type);
		m.put("eventAt", ts);
		m.put("eventAtLocal", ts.atZone(zone).toString());
		m.put("zone", zone.getId());
		m.put("text", text);
		m.put("totalGroups", total);
		m.put("totalMembers", members);
		return m;
	}

	private String formatDay(Instant instant, ZoneId zoneId) {
		return instant.atZone(zoneId).toLocalDate().toString();
	}

	private String toLower(String s) {
		return s == null ? "" : s.toLowerCase(Locale.ROOT);
	}

	private String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private int safeInt(Integer i) {
		return i == null ? 0 : i;
	}

	private long safeLong(Number n) {
		return n == null ? 0L : n.longValue();
	}

	private ZoneId safeZone(String zoneId) {
		try {
			return ZoneId.of(zoneId);
		} catch (Exception e) {
			return ZoneId.systemDefault();
		}
	}

	private long parseLongSafe(String s) {
		if (s == null || s.isBlank()) {
			return 0;
		}
		try {
			return Long.parseLong(s.replaceAll("\\D", ""));
		} catch (Exception e) {
			return 0;
		}
	}

	private Long asLong(Object v) {
		if (v == null) return null;
		if (v instanceof Number) return ((Number) v).longValue();
		try { return Long.parseLong(String.valueOf(v).replaceAll("\\D", "")); } catch (Exception e) { return null; }
	}
	private String s(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	private Integer i(Object v) {
		if (v == null) return null;
		if (v instanceof Number) return ((Number) v).intValue();
		try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
	}

	public static class DurationRange {
		private final Instant from;
		private final Instant to;

		public DurationRange(Instant from, Instant to) {
			this.from = from;
			this.to = to;
		}

		public Instant from() {
			return from;
		}

		public Instant to() {
			return to;
		}

		public static DurationRange lastHours(int hours) {
			Instant to = Instant.now();
			Instant from = to.minusSeconds(hours * 3600L);
			return new DurationRange(from, to);
		}
	}
}


