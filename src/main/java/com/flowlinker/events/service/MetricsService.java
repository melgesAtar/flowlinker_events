package com.flowlinker.events.service;

import com.flowlinker.events.projection.activity.*;
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
		long peopleReached = sumGroupMembers(customerId, from, to);
		long activePersonas = countActivePersonas(customerId, from, to);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("totalActions", actions);
		map.put("peopleReached", peopleReached);
		map.put("activePersonas", activePersonas);
		map.put("errorRate", errorRate);
		return map;
	}

	public List<Map<String, Object>> recentActivities(String customerId, int limit) {
		List<Map<String, Object>> items = new ArrayList<>();
		// share_batch
		Query qs = Query.query(Criteria.where("customerId").is(customerId))
			.with(Sort.by(Sort.Direction.DESC, "eventAt")).limit(limit);
		for (ActivityShareBatchDocument d : mongoTemplate.find(qs, ActivityShareBatchDocument.class)) {
			items.add(activityItem(d.getEventAt(), d.getAccount(), "compartilhou " + safeInt(d.getCount()) + " publicações no " + toLower(d.getPlatform())));
		}
		// errors
		for (ActivityErrorDocument d : mongoTemplate.find(qs, ActivityErrorDocument.class)) {
			items.add(activityItem(d.getEventAt(), null, "Falha: " + nullToEmpty(d.getMessage())));
		}
		// session started
		for (ActivitySessionStartedDocument d : mongoTemplate.find(qs, ActivitySessionStartedDocument.class)) {
			items.add(activityItem(d.getEventAt(), d.getAccount(), "iniciou nova sessão"));
		}
		// Ordena e corta
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
		int[][] matrix = new int[7][24]; // 0=Mon...6=Sun
		for (ActivityShareBatchDocument d : list) {
			ZonedDateTime z = d.getEventAt().atZone(now.getZone());
			int dow = (z.getDayOfWeek().getValue() % 7); // Mon=1..Sun=7 -> 0..6
			int h = z.getHour();
			matrix[dow][h] += 1;
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (int dow = 0; dow < 7; dow++) {
			for (int h = 0; h < 24; h++) {
				Map<String, Object> cell = new LinkedHashMap<>();
				cell.put("dayOfWeek", dow); // 0..6
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

	// Auxiliares
	private long countShareBatch(String customerId, Instant from, Instant to) {
		return mongoTemplate.count(timeRangeCustomer(customerId, from, to), ActivityShareBatchDocument.class);
	}
	private long countActivityErrors(String customerId, Instant from, Instant to) {
		return mongoTemplate.count(timeRangeCustomer(customerId, from, to), ActivityErrorDocument.class);
	}
	private long sumGroupMembers(String customerId, Instant from, Instant to) {
		Query q = timeRangeCustomer(customerId, from, to);
		List<ActivityShareBatchDocument> list = mongoTemplate.find(q, ActivityShareBatchDocument.class);
		long sum = 0;
		for (ActivityShareBatchDocument d : list) {
			sum += parseLongSafe(d.getGroupMembers());
		}
		return sum;
	}
	private long countActivePersonas(String customerId, Instant from, Instant to) {
		Query q1 = timeRangeCustomer(customerId, from, to);
		List<ActivitySessionStartedDocument> started = mongoTemplate.find(q1, ActivitySessionStartedDocument.class);
		List<ActivityShareBatchDocument> shares = mongoTemplate.find(q1, ActivityShareBatchDocument.class);
		Set<String> accounts = new HashSet<>();
		started.forEach(s -> { if (s.getAccount() != null) accounts.add(s.getAccount()); });
		shares.forEach(s -> { if (s.getAccount() != null) accounts.add(s.getAccount()); });
		return accounts.size();
	}
	private Query timeRangeCustomer(String customerId, Instant from, Instant to) {
		return Query.query(Criteria.where("customerId").is(customerId).and("eventAt").gte(from).lte(to));
	}
	private Map<String, Object> activityItem(Instant ts, String actor, String text) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("eventAt", ts);
		m.put("actor", actor);
		m.put("text", text);
		return m;
	}
	private String formatDay(Instant instant, ZoneId zoneId) {
		return instant.atZone(zoneId).toLocalDate().toString();
	}
	private String toLower(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
	private String nullToEmpty(String s) { return s == null ? "" : s; }
	private int safeInt(Integer i) { return i == null ? 0 : i; }
	private long parseLongSafe(String s) {
		if (s == null || s.isBlank()) return 0;
		try { return Long.parseLong(s.replaceAll("\\D", "")); } catch (Exception e) { return 0; }
	}

	public static class DurationRange {
		private final Instant from;
		private final Instant to;
		public DurationRange(Instant from, Instant to) { this.from = from; this.to = to; }
		public Instant from() { return from; }
		public Instant to() { return to; }
		public static DurationRange lastHours(int hours) {
			Instant to = Instant.now();
			Instant from = to.minusSeconds(hours * 3600L);
			return new DurationRange(from, to);
		}
	}
}


