package com.flowlinker.events.service;

import com.flowlinker.events.persistence.EventDocument;
import com.flowlinker.events.projection.activity.*;
import com.flowlinker.events.projection.campaign.CampaignCompletedDocument;
import com.flowlinker.events.projection.campaign.CampaignProgressDocument;
import com.flowlinker.events.projection.campaign.CampaignStartedDocument;
import com.flowlinker.events.service.mapper.ActivityMapperRegistry;
import com.flowlinker.events.service.mapper.EventContext;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MetricsService {

	private static final Logger log = LoggerFactory.getLogger(MetricsService.class);
    private static final Pattern CAMPAIGN_STARTED_PATTERN = Pattern.compile(
            "\\.campaign(\\.[^.]+)*\\.started$", Pattern.CASE_INSENSITIVE);

	private final MongoTemplate mongoTemplate;

	public MetricsService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

    // ==================== OVERVIEW ====================

	public Map<String, Object> overview(DurationRange range, String customerId) {
        long actions = countAllActions(customerId, range);
        long errors = countActivityErrors(customerId, range);
        double errorRate = calculateErrorRate(actions, errors);

        return Map.of(
                "totalActions", actions,
                "peopleReached", peopleReachedApprox(range, customerId),
                "activePersonas", countActivePersonas(customerId, range),
                "errorRate", errorRate
        );
	}

	public Map<String, Object> errorSummary(DurationRange range, String customerId) {
        long actions = countAllActions(customerId, range);
        long errors = countActivityErrors(customerId, range);

        return Map.of(
                "errors", errors,
                "referenceActions", actions,
                "errorRate", calculateErrorRate(actions, errors)
        );
	}

	public Map<String, Long> actionsSummary(DurationRange range, String customerId) {
        long shares = safeCount(() -> countShareBatch(customerId, range), "shares");
        long directMessages = safeCount(() -> countDirectMessages(customerId, range), "direct_messages");
        long extractions = campaignsCount(range, customerId);

        return new LinkedHashMap<>(Map.of(
                "shares", shares,
                "directMessages", directMessages,
                "extractions", extractions,
                "instagramLikes", 0L,
                "instagramComments", 0L,
                "total", shares + directMessages + extractions
        ));
    }

    // ==================== COUNTS ====================

	public long sharesCount(DurationRange range, String customerId) {
        return countShareBatch(customerId, range);
    }

    public long directMessagesCount(DurationRange range, String customerId) {
        return countDirectMessages(customerId, range);
	}

	public long campaignsCount(DurationRange range, String customerId) {
        Query query = timeRangeQuery(customerId, range)
                .addCriteria(Criteria.where("eventType").regex(CAMPAIGN_STARTED_PATTERN));

        return safeCount(() -> mongoTemplate.count(query, EventDocument.class), "campanhas");
    }

    // ==================== RECENT ACTIVITIES ====================

	public List<Map<String, Object>> recentActivities(String customerId, int limit, String zoneId) {
		ZoneId zone = safeZone(zoneId);
        List<EventDocument> events = fetchRecentEvents(customerId, limit * 4);

        return events.stream()
                .map(event -> EventContext.from(event, zone))
                .map(ActivityMapperRegistry::map)
                .filter(Objects::nonNull)
                .collect(new LimitedByTypeCollector(limit, 3));
    }

    private List<EventDocument> fetchRecentEvents(String customerId, int fetchLimit) {
		try {
			Query query = Query.query(Criteria.where("customerId").is(customerId))
                    .with(Sort.by(Sort.Direction.DESC, "eventAt"))
                    .limit(Math.max(fetchLimit, 1));
            return mongoTemplate.find(query, EventDocument.class);
		} catch (DataAccessException e) {
			log.warn("Falha ao consultar eventos brutos", e);
            return Collections.emptyList();
        }
    }

    // ==================== ACCOUNT LISTS ====================

    public List<Map<String, Object>> listAccountCreated(String customerId, int limit, String zoneId) {
        ZoneId zone = safeZone(zoneId);

        try {
            Query query = Query.query(Criteria.where("customerId").is(customerId))
                    .with(Sort.by(Sort.Direction.DESC, "eventAt"))
                    .limit(limit);

            return mongoTemplate.find(query, ActivityAccountCreatedDocument.class).stream()
                    .map(doc -> mapAccountCreated(doc, zone))
                    .toList();
        } catch (DataAccessException e) {
            log.warn("Falha ao consultar account_created", e);
            return Collections.emptyList();
        }
    }

    private Map<String, Object> mapAccountCreated(ActivityAccountCreatedDocument doc, ZoneId zone) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eventId", doc.getEventId());
        m.put("eventAt", doc.getEventAt());
        m.put("eventAtLocal", doc.getEventAt().atZone(zone).toString());
        m.put("zone", zone.getId());
        m.put("customerId", doc.getCustomerId());
        m.put("deviceId", doc.getDeviceId());
        m.put("ip", doc.getIp());
        m.put("platform", doc.getPlatform());
        m.put("account", doc.getAccount());
        m.put("profileName", doc.getProfileName());
        return m;
    }

    // ==================== EXTRACTION EVENTS ====================

    public List<Map<String, Object>> extractionEvents(String customerId, int limit, String zoneId) {
        ZoneId zone = safeZone(zoneId);
        List<Map<String, Object>> items = new ArrayList<>();

        try {
            Query query = Query.query(Criteria.where("customerId").is(customerId))
                    .with(Sort.by(Sort.Direction.DESC, "eventAt"))
                    .limit(limit);

            for (ActivityExtractionStartedDocument d : mongoTemplate.find(query, ActivityExtractionStartedDocument.class)) {
                items.add(mapExtraction("started", d.getEventAt(), zone, d.getKeywords(), d.getTotalGroups(), d.getTotalMembers()));
            }
            for (ActivityExtractionPausedDocument d : mongoTemplate.find(query, ActivityExtractionPausedDocument.class)) {
                items.add(mapExtraction("paused", d.getEventAt(), zone, d.getKeywords(), d.getTotalGroups(), d.getTotalMembers()));
            }
            for (ActivityExtractionCancelledDocument d : mongoTemplate.find(query, ActivityExtractionCancelledDocument.class)) {
                items.add(mapExtraction("cancelled", d.getEventAt(), zone, d.getKeywords(), d.getTotalGroups(), d.getTotalMembers()));
            }
            for (ActivityExtractionCompletedDocument d : mongoTemplate.find(query, ActivityExtractionCompletedDocument.class)) {
                items.add(mapExtraction("completed", d.getEventAt(), zone, d.getKeywords(), d.getTotalGroups(), d.getTotalMembers()));
            }
        } catch (DataAccessException e) {
            log.warn("Falha ao consultar eventos de extração", e);
        }

        return items.stream()
                .sorted(Comparator.comparing((Map<String, Object> m) -> (Instant) m.get("eventAt")).reversed())
                .limit(limit)
                .toList();
    }

    private Map<String, Object> mapExtraction(String type, Instant eventAt, ZoneId zone,
                                               String keywords, Long totalGroups, Long totalMembers) {
        String kw = nullToEmpty(keywords);
        long groups = safeLong(totalGroups);
        long members = safeLong(totalMembers);

        String text = switch (type) {
            case "started" -> "Extração Iniciada - Keywords: " + kw;
            case "paused" -> String.format("Extração Pausada - %d grupos, %d pessoas (keywords: %s)", groups, members, kw);
            case "cancelled" -> String.format("Extração Cancelada - %d grupos processados (keywords: %s)", groups, kw);
            case "completed" -> String.format("Extração Finalizada - %d grupos, %d pessoas (keywords: %s)", groups, members, kw);
            default -> "Extração - " + type;
        };

				Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("eventAt", eventAt);
        m.put("eventAtLocal", eventAt.atZone(zone).toString());
				m.put("zone", zone.getId());
        m.put("text", text);
        m.put("totalGroups", groups);
        m.put("totalMembers", members);
        return m;
    }

    // ==================== ANALYTICS ====================

    public Map<String, Long> distributionByNetwork(DurationRange range, String customerId) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        Set<String> processedEventIds = new HashSet<>();
        
        // Shares (Facebook) - da collection específica
        List<ActivityShareBatchDocument> shareDocs = mongoTemplate.find(
                timeRangeQuery(customerId, range), ActivityShareBatchDocument.class);
        shareDocs.forEach(d -> {
            if (d.getEventId() != null) processedEventIds.add(d.getEventId());
            String platform = Optional.ofNullable(d.getPlatform()).orElse("FACEBOOK");
            distribution.merge(platform, 1L, Long::sum);
        });
        
        // Direct Messages (Instagram) - da collection específica
        List<ActivityInstagramDirectMessageSentDocument> messageDocs = mongoTemplate.find(
                timeRangeQuery(customerId, range), ActivityInstagramDirectMessageSentDocument.class);
        messageDocs.forEach(d -> {
            if (d.getEventId() != null) processedEventIds.add(d.getEventId());
            String platform = Optional.ofNullable(d.getPlatform()).orElse("INSTAGRAM");
            distribution.merge(platform, 1L, Long::sum);
        });
        
        // Fallback: eventos brutos apenas para eventos que não estão nas collections específicas
        try {
            Query query = Query.query(Criteria.where("customerId").is(customerId)
                    .and("eventAt").gte(range.from()).lte(range.to())
                    .and("eventType").in("facebook.activity.share_batch", "facebook.activity.share.batch",
                            "instagram.activity.direct_message.sent", "instagram.direct_message.sent",
                            "desktop.activity.direct_message.sent", "desktop.activity.direct.message.sent"));

            for (EventDocument ev : mongoTemplate.find(query, EventDocument.class)) {
                // Ignora se já foi processado da collection específica
                if (ev.getEventId() != null && processedEventIds.contains(ev.getEventId())) {
                    continue;
                }
                
                Map<String, Object> p = ev.getPayload();
                if (p == null) continue;
                
                String eventType = ev.getEventType();
                String platform = null;
                
                // Determina a plataforma baseado no tipo do evento e payload
                if (eventType != null && (eventType.contains("share_batch") || eventType.contains("share.batch"))) {
                    platform = Optional.ofNullable(asString(p.get("platform"))).orElse("FACEBOOK");
                } else if (eventType != null && (eventType.contains("direct_message") || eventType.contains("direct.message"))) {
                    platform = Optional.ofNullable(asString(p.get("platform"))).orElse("INSTAGRAM");
                }
                
                if (platform != null) {
                    distribution.merge(platform, 1L, Long::sum);
                }
			}
		} catch (DataAccessException e) {
            log.warn("Falha ao consultar eventos brutos para distributionByNetwork", e);
        }
        
        return distribution;
	}

	public List<Map<String, Object>> dailyActions(String customerId, int days) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
		ZonedDateTime start = now.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0);

        Query query = Query.query(Criteria.where("customerId").is(customerId)
			.and("eventAt").gte(start.toInstant()).lte(now.toInstant()));

        Map<String, Long> sharesPerDay = mongoTemplate.find(query, ActivityShareBatchDocument.class).stream()
                .collect(Collectors.groupingBy(
                        d -> d.getEventAt().atZone(now.getZone()).toLocalDate().toString(),
			TreeMap::new,
			Collectors.counting()
		));

        Map<String, Long> messagesPerDay = mongoTemplate.find(query, ActivityInstagramDirectMessageSentDocument.class).stream()
                .collect(Collectors.groupingBy(
                        d -> d.getEventAt().atZone(now.getZone()).toLocalDate().toString(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        List<Map<String, Object>> result = new ArrayList<>();
		for (int i = 0; i < days; i++) {
            String key = start.plusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            long shares = sharesPerDay.getOrDefault(key, 0L);
            long messages = messagesPerDay.getOrDefault(key, 0L);
            result.add(Map.of(
                    "date", key,
                    "likes", 0,
                    "comments", 0,
                    "shares", shares,
                    "directMessages", messages,
                    "total", shares + messages
            ));
        }
        return result;
	}

	public List<Map<String, Object>> heatmap(String customerId, int days) {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
		ZonedDateTime start = now.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0);

        Query query = Query.query(Criteria.where("customerId").is(customerId)
			.and("eventAt").gte(start.toInstant()).lte(now.toInstant()));

		int[][] matrix = new int[7][24];
        
        // Shares (Facebook)
        for (ActivityShareBatchDocument d : mongoTemplate.find(query, ActivityShareBatchDocument.class)) {
			ZonedDateTime z = d.getEventAt().atZone(now.getZone());
			int dow = z.getDayOfWeek().getValue() % 7;
            matrix[dow][z.getHour()]++;
        }
        
        // Direct Messages (Instagram)
        for (ActivityInstagramDirectMessageSentDocument d : mongoTemplate.find(query, ActivityInstagramDirectMessageSentDocument.class)) {
            ZonedDateTime z = d.getEventAt().atZone(now.getZone());
            int dow = z.getDayOfWeek().getValue() % 7;
            matrix[dow][z.getHour()]++;
        }

        List<Map<String, Object>> result = new ArrayList<>();
		for (int dow = 0; dow < 7; dow++) {
			for (int h = 0; h < 24; h++) {
                result.add(Map.of("dayOfWeek", dow, "hour", h, "count", matrix[dow][h]));
            }
        }
        return result;
	}

	public List<Map<String, Object>> rankingPersonas(DurationRange range, String customerId, int limit) {
        Map<String, Long> actionsByAccount = new HashMap<>();
        
        // Shares (Facebook)
        List<ActivityShareBatchDocument> shareDocs = mongoTemplate.find(
                timeRangeQuery(customerId, range), ActivityShareBatchDocument.class);
        shareDocs.forEach(d -> {
            String account = Optional.ofNullable(d.getAccount()).orElse("desconhecido");
            actionsByAccount.merge(account, 1L, Long::sum);
        });
        
        // Direct Messages (Instagram)
        List<ActivityInstagramDirectMessageSentDocument> messageDocs = mongoTemplate.find(
                timeRangeQuery(customerId, range), ActivityInstagramDirectMessageSentDocument.class);
        messageDocs.forEach(d -> {
            String account = Optional.ofNullable(d.getAccount()).orElse("desconhecido");
            actionsByAccount.merge(account, 1L, Long::sum);
        });

        return actionsByAccount.entrySet().stream()
			.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
			.limit(limit)
                .map(e -> Map.<String, Object>of("account", e.getKey(), "actions", e.getValue()))
                .toList();
    }

    public long peopleReachedApprox(DurationRange range, String customerId) {
        Map<String, Long> membersByGroup = new HashMap<>();

        // Projeção tipada
        try {
            for (ActivityShareBatchDocument d : mongoTemplate.find(
                    timeRangeQuery(customerId, range).with(Sort.by(Sort.Direction.DESC, "eventAt")),
                    ActivityShareBatchDocument.class)) {

                String key = Optional.ofNullable(d.getGroupUrl()).filter(s -> !s.isBlank())
                        .orElse(Optional.ofNullable(d.getGroupName()).orElse(""));
                if (!key.isBlank()) {
                    membersByGroup.putIfAbsent(key, parseLongSafe(d.getGroupMembers()));
                }
            }
		} catch (DataAccessException e) {
			log.warn("Falha ao calcular peopleReached (projeção)", e);
		}

        // Fallback: eventos brutos
		try {
            Query query = Query.query(Criteria.where("customerId").is(customerId)
                    .and("eventAt").gte(range.from()).lte(range.to())
				.and("eventType").in("facebook.activity.share_batch", "facebook.activity.share.batch"))
				.with(Sort.by(Sort.Direction.DESC, "eventAt"));

            for (EventDocument ev : mongoTemplate.find(query, EventDocument.class)) {
                Map<String, Object> p = ev.getPayload();
                if (p == null) continue;

                String groupUrl = asString(p.get("groupUrl"));
                String groupName = asString(p.get("groupName"));
                String key = (groupUrl != null && !groupUrl.isBlank()) ? groupUrl : nullToEmpty(groupName);
                if (key.isBlank()) continue;

                Long members = asLong(p.get("groupMembers"));
                if (members != null) {
                    membersByGroup.putIfAbsent(key, members);
                }
            }
		} catch (DataAccessException e) {
			log.warn("Falha ao calcular peopleReached (bruto)", e);
		}

        return membersByGroup.values().stream()
                .mapToLong(members -> Math.round(members * 0.30d))
                .sum();
    }

    // ==================== ACCOUNT STATUS ====================

    public Optional<Map<String, Object>> lastDeviceForAccount(String customerId, String account, String zoneId) {
        if (isBlank(customerId) || isBlank(account)) return Optional.empty();

        ZoneId zone = safeZone(zoneId);

        return findLatest(customerId, account, ActivitySessionStartedDocument.class)
                .map(doc -> usageInfo(doc.getEventAt(), doc.getReceivedAt(), zone))
                .or(() -> findLatest(customerId, account, ActivityShareBatchDocument.class)
                        .map(doc -> usageInfo(doc.getEventAt(), doc.getReceivedAt(), zone)))
                .or(() -> findLatest(customerId, account, ActivityInstagramDirectMessageSentDocument.class)
                        .map(doc -> usageInfo(doc.getEventAt(), doc.getReceivedAt(), zone)));
    }

    public Optional<Map<String, Object>> accountSuspensionStatus(String customerId, String account, String zoneId) {
        if (isBlank(customerId) || isBlank(account)) return Optional.empty();

        ZoneId zone = safeZone(zoneId);

        return findLatest(customerId, account, ActivityAccountSuspendedDocument.class)
                .map(doc -> buildDurationStatus(doc.getEventAt(), doc.getReceivedAt(), zone, "suspended", doc.getReason()));
    }

    public Optional<Map<String, Object>> accountBlockStatus(String customerId, String account, String zoneId) {
        if (isBlank(customerId) || isBlank(account)) return Optional.empty();

        ZoneId zone = safeZone(zoneId);

        return findLatest(customerId, account, ActivityAccountBlockedDocument.class)
                .map(doc -> buildDurationStatus(doc.getEventAt(), doc.getReceivedAt(), zone, "blocked", doc.getReason()));
    }

    // ==================== PRIVATE HELPERS ====================

    private long countShareBatch(String customerId, DurationRange range) {
        return safeCount(() -> mongoTemplate.count(timeRangeQuery(customerId, range), ActivityShareBatchDocument.class), "shares");
    }

    private long countDirectMessages(String customerId, DurationRange range) {
        return safeCount(() -> mongoTemplate.count(timeRangeQuery(customerId, range), ActivityInstagramDirectMessageSentDocument.class), "direct_messages");
    }

    private long countAllActions(String customerId, DurationRange range) {
        return countShareBatch(customerId, range) + countDirectMessages(customerId, range);
    }

    private long countActivityErrors(String customerId, DurationRange range) {
        return safeCount(() -> mongoTemplate.count(timeRangeQuery(customerId, range), ActivityErrorDocument.class), "errors");
    }

    private long countActivePersonas(String customerId, DurationRange range) {
        Query query = timeRangeQuery(customerId, range);
        Set<String> accounts = new HashSet<>();

        mongoTemplate.find(query, ActivitySessionStartedDocument.class)
                .forEach(s -> { if (s.getAccount() != null) accounts.add(s.getAccount()); });
        mongoTemplate.find(query, ActivityShareBatchDocument.class)
                .forEach(s -> { if (s.getAccount() != null) accounts.add(s.getAccount()); });
        mongoTemplate.find(query, ActivityInstagramDirectMessageSentDocument.class)
                .forEach(s -> { if (s.getAccount() != null) accounts.add(s.getAccount()); });

        return accounts.size();
    }

    private <T> Optional<T> findLatest(String customerId, String account, Class<T> type) {
        try {
            Query query = Query.query(Criteria.where("customerId").is(customerId).and("account").is(account))
                    .with(Sort.by(Sort.Direction.DESC, "eventAt"))
                    .limit(1);
            return Optional.ofNullable(mongoTemplate.findOne(query, type));
        } catch (DataAccessException e) {
            log.warn("Falha ao consultar mais recente de {}", type.getSimpleName(), e);
            return Optional.empty();
        }
    }

    private Query timeRangeQuery(String customerId, DurationRange range) {
        return Query.query(Criteria.where("customerId").is(customerId)
                .and("eventAt").gte(range.from()).lte(range.to()));
    }

    private Map<String, Object> usageInfo(Instant eventAt, Instant receivedAt, ZoneId zone) {
        Instant ts = eventAt != null ? eventAt : receivedAt;
        return Map.of(
                "lastUsedAt", ts,
                "lastUsedAtLocal", ts != null ? ts.atZone(zone).toString() : null
        );
    }

    private Map<String, Object> buildDurationStatus(Instant eventAt, Instant receivedAt, ZoneId zone,
                                                     String prefix, String reason) {
        Instant startedAt = eventAt != null ? eventAt : receivedAt;
        if (startedAt == null) return null;

        Instant now = Instant.now();
        long seconds = Duration.between(startedAt, now).getSeconds();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put(prefix + "Since", startedAt);
        data.put(prefix + "SinceLocal", startedAt.atZone(zone).toString());
        data.put("now", now);
        data.put("nowLocal", now.atZone(zone).toString());
        data.put("durationSeconds", seconds);
        data.put("durationMinutes", seconds / 60);
        data.put("durationHours", seconds / 3600);
        data.put("reason", reason);
        return data;
    }

    private double calculateErrorRate(long actions, long errors) {
        return (actions + errors) == 0 ? 0.0 : (errors * 100.0) / (actions + errors);
    }

    private long safeCount(java.util.function.LongSupplier supplier, String label) {
        try {
            return supplier.getAsLong();
        } catch (DataAccessException e) {
            log.warn("Falha ao contar {}", label, e);
            return 0;
        }
	}

	private ZoneId safeZone(String zoneId) {
		try {
			return ZoneId.of(zoneId);
		} catch (Exception e) {
			return ZoneId.systemDefault();
		}
	}

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private long safeLong(Number n) {
        return n == null ? 0L : n.longValue();
    }

    private long parseLongSafe(String s) {
        if (s == null || s.isBlank()) return 0;
		try {
			return Long.parseLong(s.replaceAll("\\D", ""));
		} catch (Exception e) {
			return 0;
		}
	}

    private String asString(Object v) {
        return v == null ? null : String.valueOf(v);
	}

	private Long asLong(Object v) {
		if (v == null) return null;
		if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v).replaceAll("\\D", ""));
        } catch (Exception e) {
		return null;
	}
    }

    // ==================== INNER CLASSES ====================

    /**
     * Collector que limita o total de itens e também limita por tipo.
     */
    private static class LimitedByTypeCollector implements java.util.stream.Collector<
            Map<String, Object>,
            LimitedByTypeCollector.Accumulator,
            List<Map<String, Object>>> {

        private final int totalLimit;
        private final int perTypeLimit;

        LimitedByTypeCollector(int totalLimit, int perTypeLimit) {
            this.totalLimit = totalLimit;
            this.perTypeLimit = perTypeLimit;
        }

        @Override
        public java.util.function.Supplier<Accumulator> supplier() {
            return () -> new Accumulator(totalLimit, perTypeLimit);
        }

        @Override
        public java.util.function.BiConsumer<Accumulator, Map<String, Object>> accumulator() {
            return Accumulator::add;
        }

        @Override
        public java.util.function.BinaryOperator<Accumulator> combiner() {
            return (a, b) -> {
                b.items.forEach(a::add);
                return a;
            };
        }

        @Override
        public java.util.function.Function<Accumulator, List<Map<String, Object>>> finisher() {
            return acc -> acc.items;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }

        static class Accumulator {
            final List<Map<String, Object>> items = new ArrayList<>();
            final Map<String, Integer> typeCounts = new HashMap<>();
            final int totalLimit;
            final int perTypeLimit;

            Accumulator(int totalLimit, int perTypeLimit) {
                this.totalLimit = totalLimit;
                this.perTypeLimit = perTypeLimit;
            }

            void add(Map<String, Object> item) {
                if (items.size() >= totalLimit) return;

                String type = String.valueOf(item.getOrDefault("type", item.get("eventType")));
                if (type == null || type.isBlank()) type = "generic";

                int count = typeCounts.getOrDefault(type, 0);
                if (count >= perTypeLimit) return;

                typeCounts.put(type, count + 1);
                items.add(item);
            }
        }
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

        public static DurationRange between(Instant from, Instant to) {
            if (from == null || to == null) throw new IllegalArgumentException("start and end must be provided");
            if (from.isAfter(to)) throw new IllegalArgumentException("start must be before end");
            long days = ChronoUnit.DAYS.between(from, to);
            if (days > 60) throw new IllegalArgumentException("O período máximo permitido é de 60 dias");
            return new DurationRange(from, to);
        }
	}

    // ==================== CAMPAIGNS DETAILS ====================

    public List<Map<String, Object>> listCampaignsWithActivities(DurationRange range, String customerId, String zoneId) {
        ZoneId zone = safeZone(zoneId);
        Map<Long, Map<String, Object>> campaigns = new LinkedHashMap<>();

        try {
            Query common = timeRangeQuery(customerId, range).with(Sort.by(Sort.Direction.DESC, "eventAt"));

            for (CampaignStartedDocument d : mongoTemplate.find(common, CampaignStartedDocument.class)) {
                Long cid = d.getCampaignId();
                if (cid == null) continue;
                Map<String, Object> c = campaigns.computeIfAbsent(cid, k -> new LinkedHashMap<>());
                c.putIfAbsent("campaignId", cid);
                c.putIfAbsent("platform", d.getPlatform());
                c.putIfAbsent("campaignType", d.getCampaignType());
                c.putIfAbsent("startedAt", d.getEventAt());
                c.putIfAbsent("startedAtLocal", d.getEventAt() != null ? d.getEventAt().atZone(zone).toString() : null);
                c.putIfAbsent("startedEventId", d.getEventId());
                c.putIfAbsent("total", d.getTotal());
            }

            for (CampaignProgressDocument d : mongoTemplate.find(common, CampaignProgressDocument.class)) {
                Long cid = d.getCampaignId();
                if (cid == null) continue;
                Map<String, Object> c = campaigns.computeIfAbsent(cid, k -> new LinkedHashMap<>());
                Object existing = c.get("lastProgressAt");
                if (existing == null || ((Instant) existing).isBefore(d.getEventAt())) {
                    c.put("lastProgressAt", d.getEventAt());
                    c.put("lastProgressAtLocal", d.getEventAt() != null ? d.getEventAt().atZone(zone).toString() : null);
                    c.put("lastProcessedIndex", d.getLastProcessedIndex());
                    c.put("progressEventId", d.getEventId());
                }
            }

            for (CampaignCompletedDocument d : mongoTemplate.find(common, CampaignCompletedDocument.class)) {
                Long cid = d.getCampaignId();
                if (cid == null) continue;
                Map<String, Object> c = campaigns.computeIfAbsent(cid, k -> new LinkedHashMap<>());
                c.put("completedAt", d.getEventAt());
                c.put("completedAtLocal", d.getEventAt() != null ? d.getEventAt().atZone(zone).toString() : null);
                c.put("completedEventId", d.getEventId());
                c.putIfAbsent("total", d.getTotal());
            }
        } catch (DataAccessException e) {
            log.warn("Falha ao consultar projeções de campanha", e);
        }

        // tenta extrair campaignName de eventos brutos quando disponível
        for (Long cid : new ArrayList<>(campaigns.keySet())) {
            Map<String, Object> c = campaigns.get(cid);
            try {
                Query q = Query.query(Criteria.where("customerId").is(customerId)
                        .and("eventAt").gte(range.from()).lte(range.to())
                        .and("payload.campaignId").is(cid))
                        .with(Sort.by(Sort.Direction.DESC, "eventAt"))
                        .limit(5);

                for (EventDocument ev : mongoTemplate.find(q, EventDocument.class)) {
                    Map<String, Object> p = ev.getPayload();
                    if (p == null) continue;
                    String name = Optional.ofNullable(asString(p.get("name")))
                            .or(() -> Optional.ofNullable(asString(p.get("campaignName"))))
                            .or(() -> Optional.ofNullable(asString(p.get("title"))))
                            .orElse(null);
                    if (name != null) c.putIfAbsent("campaignName", name);
                    if (c.get("campaignName") != null) break;
                }
            } catch (DataAccessException e) {
                log.debug("Falha ao buscar nome da campanha em eventos brutos para {}", cid, e);
            }
        }

        // associa messages e shares por campaignId
        for (Map.Entry<Long, Map<String, Object>> entry : campaigns.entrySet()) {
            Long cid = entry.getKey();
            Map<String, Object> c = entry.getValue();

            List<Map<String, Object>> messages = new ArrayList<>();
            try {
                Query mq = Query.query(Criteria.where("customerId").is(customerId)
                        .and("campaignId").is(cid)
                        .and("eventAt").gte(range.from()).lte(range.to()))
                        .with(Sort.by(Sort.Direction.DESC, "eventAt"));
                for (ActivityInstagramDirectMessageSentDocument d : mongoTemplate.find(mq, ActivityInstagramDirectMessageSentDocument.class)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("eventId", d.getEventId());
                    m.put("eventAt", d.getEventAt());
                    m.put("eventAtLocal", d.getEventAt() != null ? d.getEventAt().atZone(zone).toString() : null);
                    m.put("account", d.getAccount());
                    m.put("recipientUsername", d.getRecipientUsername());
                    m.put("recipientId", d.getRecipientId());
                    m.put("messagePreview", d.getMessagePreview());
                    m.put("source", d.getSource());
                    m.put("deviceId", d.getDeviceId());
                    m.put("ip", d.getIp());
                    messages.add(m);
                }
            } catch (DataAccessException ex) {
                log.warn("Falha ao consultar mensagens diretas para campanha {}", cid, ex);
            }

            List<Map<String, Object>> shares = new ArrayList<>();
            try {
                // procura eventos brutos de share com payload.campaignId
                Query sq = Query.query(Criteria.where("customerId").is(customerId)
                        .and("eventAt").gte(range.from()).lte(range.to())
                        .and("eventType").in("facebook.activity.share_batch", "facebook.activity.share.batch",
                                "instagram.activity.share_batch", "instagram.activity.share.batch")
                        .and("payload.campaignId").is(cid))
                        .with(Sort.by(Sort.Direction.DESC, "eventAt"));

                for (EventDocument ev : mongoTemplate.find(sq, EventDocument.class)) {
                    Map<String, Object> p = ev.getPayload();
                    if (p == null) continue;
                    Map<String, Object> s = new LinkedHashMap<>();
                    s.put("eventId", ev.getEventId());
                    s.put("eventAt", ev.getEventAt());
                    s.put("eventAtLocal", ev.getEventAt() != null ? ev.getEventAt().atZone(zone).toString() : null);
                    s.put("account", asString(p.get("account")));
                    s.put("groupName", asString(p.get("groupName")));
                    s.put("groupUrl", asString(p.get("groupUrl")));
                    s.put("groupMembers", asString(p.get("groupMembers")));
                    s.put("post", asString(p.get("post")));
                    shares.add(s);
                }
            } catch (DataAccessException ex) {
                log.warn("Falha ao consultar shares relacionados a campanha {}", cid, ex);
            }

            c.put("directMessages", messages);
            c.put("shares", shares);
            c.put("directMessagesCount", messages.size());
            c.put("sharesCount", shares.size());
        }

        return campaigns.values().stream()
                .sorted((a, b) -> {
                    Instant ia = (Instant) a.getOrDefault("startedAt", a.get("lastProgressAt"));
                    Instant ib = (Instant) b.getOrDefault("startedAt", b.get("lastProgressAt"));
                    if (ia == null && ib == null) return 0;
                    if (ia == null) return 1;
                    if (ib == null) return -1;
                    return ib.compareTo(ia);
                })
                .toList();
    }


}
