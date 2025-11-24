package com.flowlinker.events.service;

import com.flowlinker.events.persistence.EventDocument;
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
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class MetricsService {

	private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

	private static final Pattern CAMPAIGN_STARTED_PATTERN = Pattern.compile("\\.campaign(\\.[^.]+)*\\.started$", Pattern.CASE_INSENSITIVE);

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

	public Map<String, Object> errorSummary(DurationRange range, String customerId) {
		Instant from = range.from();
		Instant to = range.to();
		long actions = countShareBatch(customerId, from, to);
		long errors = countActivityErrors(customerId, from, to);
		double errorRate = (actions + errors) == 0 ? 0.0 : (errors * 100.0) / (actions + errors);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("errors", errors);
		map.put("referenceActions", actions);
		map.put("errorRate", errorRate);
		return map;
	}

	public Map<String, Long> actionsSummary(DurationRange range, String customerId) {
		Instant from = range.from();
		Instant to = range.to();
		long shares = 0;
		long extractions;
		try {
			shares = countShareBatch(customerId, from, to);
		} catch (DataAccessException e) {
			log.warn("Falha ao contar shares", e);
		}
		extractions = campaignsCount(range, customerId);
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

	public long sharesCount(DurationRange range, String customerId) {
		Instant from = range.from();
		Instant to = range.to();
		return countShareBatch(customerId, from, to);
	}

	public long campaignsCount(DurationRange range, String customerId) {
		Instant from = range.from();
		Instant to = range.to();
		Query query = timeRangeCustomer(customerId, from, to);
		query.addCriteria(Criteria.where("eventType").regex(CAMPAIGN_STARTED_PATTERN));
		try {
			return mongoTemplate.count(query, EventDocument.class);
		} catch (DataAccessException e) {
			log.warn("Falha ao contar campanhas iniciadas", e);
			return 0;
		}
	}

	public List<Map<String, Object>> recentActivities(String customerId, int limit, String zoneId) {
		ZoneId zone = safeZone(zoneId);
		List<Map<String, Object>> items = new ArrayList<>();
		Sort sort = Sort.by(Sort.Direction.DESC, "eventAt");
		List<EventDocument> events = Collections.emptyList();
		// controla quantas vezes cada tipo de atividade aparece para evitar poluição visual
		Map<String, Integer> typeCounts = new HashMap<>();

		try {
			Query query = Query.query(Criteria.where("customerId").is(customerId))
				.with(sort)
				.limit(Math.max(limit, 1) * 4);
			events = mongoTemplate.find(query, EventDocument.class);
		} catch (DataAccessException e) {
			log.warn("Falha ao consultar eventos brutos", e);
		}

		for (EventDocument event : events) {
			Map<String, Object> mapped = mapRecentActivity(event, zone);
			if (mapped == null) {
				continue;
			}
			String key = String.valueOf(mapped.getOrDefault("type", mapped.get("eventType")));
			if (key == null || key.isBlank()) {
				key = "generic";
			}
			int current = typeCounts.getOrDefault(key, 0);
			if (current >= 3) {
				// já temos muitas ocorrências deste tipo; pula para não poluir a lista
				continue;
			}
			typeCounts.put(key, current + 1);
			items.add(mapped);
			if (items.size() >= limit) {
				break;
			}
		}
		return items;
	}

	private Map<String, Object> mapRecentActivity(EventDocument event, ZoneId zone) {
		if (event == null || event.getEventType() == null) {
			return null;
		}

		String type = event.getEventType();
		Map<String, Object> payload = event.getPayload() == null ? Collections.emptyMap() : event.getPayload();
		String deviceId = event.getDeviceId();
		String ip = event.getIp();
		String eventId = event.getEventId();
		Instant ts = Optional.ofNullable(event.getEventAt()).orElse(event.getReceivedAt());
		if (ts == null) {
			ts = Instant.now();
		}

		// identificador de conta: tenta "account", depois "username" e por fim "name"
		String account = Optional.ofNullable(s(payload.get("account")))
			.map(String::trim).filter(v -> !v.isEmpty())
			.orElseGet(() -> Optional.ofNullable(s(payload.get("username")))
				.map(String::trim).filter(v -> !v.isEmpty())
				.orElseGet(() -> Optional.ofNullable(s(payload.get("name")))
					.map(String::trim).filter(v -> !v.isEmpty()).orElse(null)));
		String platform = Optional.ofNullable(s(payload.get("platform"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
		if (platform == null) {
			platform = Optional.ofNullable(s(payload.get("source"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
		}

		// Eventos de login - sucesso (modelo novo + legado desktop)
		if ("security.auth.login".equals(type)
			|| "auth.security.login".equals(type)
			|| "desktop.security.login_success".equals(type)
			|| "desktop.security.login.success".equals(type)) {
			String username = Optional.ofNullable(s(payload.get("username"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String acct = Optional.ofNullable(account).filter(v -> !v.isBlank())
				.orElse(Optional.ofNullable(s(payload.get("account"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null));
			String actor = username != null
				? username
				: (acct != null ? acct : Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("desconhecido"));
			String source = Optional.ofNullable(s(payload.get("source"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String effectivePlatform = Optional.ofNullable(platform).filter(v -> !v.isBlank())
				.orElseGet(() -> {
					if (type.startsWith("desktop")) return "DESKTOP";
					if (type.startsWith("facebook")) return "FACEBOOK";
					if ("device".equalsIgnoreCase(source)) return "DEVICE";
					if ("web".equalsIgnoreCase(source)) return "WEB";
					return "DESCONHECIDO";
				});

			// mensagem específica para device x web
			// prioridade: deviceName (novo) -> nameDevice (legado) -> hostname -> deviceId
			String deviceName = Optional.ofNullable(s(payload.get("deviceName")))
				.map(String::trim).filter(v -> !v.isEmpty())
				.orElseGet(() -> Optional.ofNullable(s(payload.get("nameDevice")))
					.map(String::trim).filter(v -> !v.isEmpty())
					.orElseGet(() -> Optional.ofNullable(s(payload.get("hostname")))
						.map(String::trim).filter(v -> !v.isEmpty()).orElse(null)));
			String text;
			if ("web".equalsIgnoreCase(source)) {
				String ipUser = Optional.ofNullable(ip).filter(v -> !v.isBlank()).orElse("IP desconhecido");
				text = String.format("Login realizado com sucesso no painel web IP: %s", ipUser);
			} else {
				String name = deviceName != null
					? deviceName
					: Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("dispositivo");
				text = String.format("Login realizado com sucesso no dispositivo: %s", name);
			}

			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "login.success");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("username", username);
			entry.put("account", acct);
			entry.put("source", source);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("security.auth.login_failed".equals(type)
			|| "auth.security.login_failed".equals(type)
			|| "desktop.security.login_failed".equals(type)
			|| "desktop.security.login.failed".equals(type)) {
			String username = Optional.ofNullable(s(payload.get("username"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String acct = Optional.ofNullable(account).filter(v -> !v.isBlank())
				.orElse(Optional.ofNullable(s(payload.get("account"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null));
			String actor = username != null
				? username
				: (acct != null ? acct : Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("desconhecido"));

			String source = Optional.ofNullable(s(payload.get("source"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);

			String reason = Optional.ofNullable(s(payload.get("reason"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String message = Optional.ofNullable(s(payload.get("message"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);

			// Tenta usar mensagem já em português, senão traduz o reason
			String reasonLabel;
			if (message != null) {
				reasonLabel = message;
			} else if (reason != null) {
				String r = reason.toLowerCase(Locale.ROOT);
				if ("invalid_credentials".equals(r) || "invalidcredential".equals(r)) {
					reasonLabel = "credenciais inválidas";
				} else if ("access_denied".equals(r)) {
					reasonLabel = "acesso negado";
				} else if ("server_unavailable".equals(r) || "service_unavailable".equals(r)) {
					reasonLabel = "servidor indisponível";
				} else if ("account_locked".equals(r)) {
					reasonLabel = "conta bloqueada";
				} else if ("account_disabled".equals(r)) {
					reasonLabel = "conta desativada";
				} else {
					reasonLabel = reason;
				}
			} else {
				reasonLabel = "motivo desconhecido";
			}

			// nome de dispositivo (quando source=device ou desktop)
			// prioridade: deviceName (novo) -> nameDevice (legado) -> hostname -> deviceId
			String deviceName = Optional.ofNullable(s(payload.get("deviceName")))
				.map(String::trim).filter(v -> !v.isEmpty())
				.orElseGet(() -> Optional.ofNullable(s(payload.get("nameDevice")))
					.map(String::trim).filter(v -> !v.isEmpty())
					.orElseGet(() -> Optional.ofNullable(s(payload.get("hostname")))
						.map(String::trim).filter(v -> !v.isEmpty()).orElse(null)));

			String text;
			if ("web".equalsIgnoreCase(source)) {
				String ipUser = Optional.ofNullable(ip).filter(v -> !v.isBlank()).orElse("IP desconhecido");
				text = String.format("Falha no login feito no painel web IP: %s, motivo: %s", ipUser, reasonLabel);
			} else {
				String name = deviceName != null
					? deviceName
					: Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("dispositivo");
				text = String.format("Falha no login feito no dispositivo: %s, motivo: %s", name, reasonLabel);
			}

			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "login.failed");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("username", username);
			entry.put("account", acct);
			entry.put("reason", reason);
			entry.put("message", message);
			entry.put("source", source);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("auth.security.device_changed".equals(type)
			|| "security.auth.device_changed".equals(type)) {
			String username = Optional.ofNullable(s(payload.get("username"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String actor = username != null
				? username
				: Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("desconhecido");
			String source = Optional.ofNullable(s(payload.get("source"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String effectivePlatform = Optional.ofNullable(platform).filter(v -> !v.isBlank())
				.orElseGet(() -> {
					if ("device".equalsIgnoreCase(source)) return "DEVICE";
					if ("web".equalsIgnoreCase(source)) return "WEB";
					return "DESCONHECIDO";
				});
			String text = String.format("Mudança de dispositivo detectada (%s)", effectivePlatform);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "device.changed");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("username", username);
			entry.put("source", source);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if (type.contains(".campaign.") && type.endsWith(".started")) {
			String actor = account == null || account.isBlank()
				? Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("sistema")
				: account;
			String effectivePlatform = platform;
			if (effectivePlatform == null || effectivePlatform.isBlank()) {
				String[] parts = type.split("\\.");
				if (parts.length > 0) {
					effectivePlatform = parts[0].toUpperCase(Locale.ROOT);
				} else {
					effectivePlatform = "DESCONHECIDO";
				}
			}
			Long campaignId = asLong(payload.get("campaignId"));
			Integer total = i(payload.get("total"));
			String text = String.format("Campanha iniciada (%s)", effectivePlatform);
			if (total != null) {
				text = String.format("Campanha iniciada (%s) - %d itens previstos", effectivePlatform, total);
			}
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "campaign.started");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			if (campaignId != null) {
				entry.put("campaignId", campaignId);
			}
			if (total != null) {
				entry.put("total", total);
			}
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if (type.contains(".campaign.") && type.endsWith(".progress")) {
			String actor = account == null || account.isBlank()
				? Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("sistema")
				: account;
			String effectivePlatform = platform;
			if (effectivePlatform == null || effectivePlatform.isBlank()) {
				String[] parts = type.split("\\.");
				if (parts.length > 0) {
					effectivePlatform = parts[0].toUpperCase(Locale.ROOT);
				} else {
					effectivePlatform = "DESCONHECIDO";
				}
			}
			Long campaignId = asLong(payload.get("campaignId"));
			Long lastIndex = asLong(payload.get("lastProcessedIndex"));
			Integer total = i(payload.get("total"));
			String text;
			if (lastIndex != null && total != null) {
				text = String.format("Campanha em andamento (%s) - %d de %d itens", effectivePlatform, lastIndex, total);
			} else if (lastIndex != null) {
				text = String.format("Campanha em andamento (%s) - %d itens processados", effectivePlatform, lastIndex);
			} else {
				text = String.format("Campanha em andamento (%s)", effectivePlatform);
			}
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "campaign.progress");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			if (campaignId != null) {
				entry.put("campaignId", campaignId);
			}
			if (lastIndex != null) {
				entry.put("lastProcessedIndex", lastIndex);
			}
			if (total != null) {
				entry.put("total", total);
			}
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if (type.contains(".campaign.") && type.endsWith(".completed")) {
			String actor = account == null || account.isBlank()
				? Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("sistema")
				: account;
			String effectivePlatform = platform;
			if (effectivePlatform == null || effectivePlatform.isBlank()) {
				String[] parts = type.split("\\.");
				if (parts.length > 0) {
					effectivePlatform = parts[0].toUpperCase(Locale.ROOT);
				} else {
					effectivePlatform = "DESCONHECIDO";
				}
			}
			Long campaignId = asLong(payload.get("campaignId"));
			Long lastIndex = asLong(payload.get("lastProcessedIndex"));
			Integer total = i(payload.get("total"));
			String text;
			if (lastIndex != null && total != null) {
				text = String.format("Campanha concluída (%s) - %d de %d itens", effectivePlatform, lastIndex, total);
			} else {
				text = String.format("Campanha concluída (%s)", effectivePlatform);
			}
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "campaign.completed");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			if (campaignId != null) {
				entry.put("campaignId", campaignId);
			}
			if (lastIndex != null) {
				entry.put("lastProcessedIndex", lastIndex);
			}
			if (total != null) {
				entry.put("total", total);
			}
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		}

		if ("facebook.activity.share_batch".equals(type) || "facebook.activity.share.batch".equals(type)) {
			String actor = account == null || account.isBlank() ? "desconhecido" : account;
			String effectivePlatform = platform == null || platform.isBlank() ? "FACEBOOK" : platform;
			String groupName = Optional.ofNullable(s(payload.get("groupName"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String text = groupName == null
				? String.format("Compartilhamento realizado (%s)", effectivePlatform)
				: String.format("Compartilhamento no grupo %s (%s)", groupName, effectivePlatform);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "share");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("account", actor);
			entry.put("groupName", groupName);
			entry.put("groupUrl", Optional.ofNullable(s(payload.get("groupUrl"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null));
			entry.put("post", Optional.ofNullable(s(payload.get("post"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null));
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("desktop.activity.session_started".equals(type) || "facebook.activity.session_started".equals(type)) {
			String actor = account == null || account.isBlank() ? Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("desconhecido") : account;
			String effectivePlatform = platform == null || platform.isBlank()
				? (type.startsWith("desktop") ? "DESKTOP" : "FACEBOOK")
				: platform;
			String text = String.format("Sessão iniciada (%s)", effectivePlatform);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "session.started");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("account", actor);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("desktop.activity.session_ended".equals(type) || "facebook.activity.session_ended".equals(type)) {
			String actor = account == null || account.isBlank() ? Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("desconhecido") : account;
			String effectivePlatform = platform == null || platform.isBlank()
				? (type.startsWith("desktop") ? "DESKTOP" : "FACEBOOK")
				: platform;
			String reason = Optional.ofNullable(s(payload.get("reason"))).map(String::trim).filter(v -> !v.isEmpty()).orElse("motivo desconhecido");
			String text = String.format("Sessão encerrada (%s) - %s", effectivePlatform, reason);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "session.ended");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("account", actor);
			entry.put("reason", reason);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("facebook.activity.error".equals(type) || "desktop.activity.error".equals(type)) {
			String actor = account == null || account.isBlank()
				? Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("sistema")
				: account;
			String code = Optional.ofNullable(s(payload.get("code"))).map(String::trim).filter(v -> !v.isEmpty()).orElse("desconhecido");
			String message = Optional.ofNullable(s(payload.get("message"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String text = message == null
				? String.format("Erro reportado (%s)", code)
				: String.format("Erro reportado (%s): %s", code, message);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "error");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("code", code);
			entry.put("message", message);
			Object ctx = payload.get("context");
			if (ctx instanceof Map) {
				entry.put("context", ctx);
			}
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("facebook.activity.extraction.started".equals(type)) {
			Map<String, Object> entry = extractionItem("extraction.started", ts, zone,
				String.format("Extração iniciada - keywords: %s", Optional.ofNullable(s(payload.get("keywords"))).map(String::trim).orElse("")),
				Optional.ofNullable(asLong(payload.get("totalGroups"))).orElse(0L),
				Optional.ofNullable(asLong(payload.get("totalMembers"))).orElse(0L));
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("facebook.activity.extraction.paused".equals(type)) {
			long totalGroups = Optional.ofNullable(asLong(payload.get("totalGroups"))).orElse(0L);
			long totalMembers = Optional.ofNullable(asLong(payload.get("totalMembers"))).orElse(0L);
			String text = String.format("Extração pausada - %d grupos, %d pessoas", totalGroups, totalMembers);
			Map<String, Object> entry = extractionItem("extraction.paused", ts, zone, text, totalGroups, totalMembers);
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("facebook.activity.extraction.cancelled".equals(type)) {
			long totalGroups = Optional.ofNullable(asLong(payload.get("totalGroups"))).orElse(0L);
			String text = String.format("Extração cancelada - %d grupos processados", totalGroups);
			Map<String, Object> entry = extractionItem("extraction.cancelled", ts, zone, text, totalGroups,
				Optional.ofNullable(asLong(payload.get("totalMembers"))).orElse(0L));
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("facebook.activity.extraction.completed".equals(type)) {
			long totalGroups = Optional.ofNullable(asLong(payload.get("totalGroups"))).orElse(0L);
			long totalMembers = Optional.ofNullable(asLong(payload.get("totalMembers"))).orElse(0L);
			String text = String.format("Extração concluída - %d grupos, %d pessoas", totalGroups, totalMembers);
			Map<String, Object> entry = extractionItem("extraction.completed", ts, zone, text, totalGroups, totalMembers);
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("device.activity.created".equals(type)) {
			String fp = Optional.ofNullable(s(payload.get("fingerprint"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String osName = Optional.ofNullable(s(payload.get("osName"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String appVersion = Optional.ofNullable(s(payload.get("appVersion"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String actor = Optional.ofNullable(deviceId).filter(v -> !v.isBlank())
				.orElse(fp != null ? fp : "dispositivo");
			StringBuilder sb = new StringBuilder("Novo dispositivo registrado");
			if (osName != null || appVersion != null) {
				sb.append(" (");
				if (osName != null) {
					sb.append(osName);
				}
				if (appVersion != null) {
					if (osName != null) sb.append(" - ");
					sb.append("versão ").append(appVersion);
				}
				sb.append(")");
			}
			String text = sb.toString();
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "device.created");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("fingerprint", fp);
			entry.put("osName", osName);
			entry.put("appVersion", appVersion);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("device.activity.renamed".equals(type)) {
			String oldName = Optional.ofNullable(s(payload.get("oldName"))).map(String::trim).filter(v -> !v.isEmpty()).orElse("nome anterior");
			String newName = Optional.ofNullable(s(payload.get("newName"))).map(String::trim).filter(v -> !v.isEmpty()).orElse("novo nome");
			String actor = Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("dispositivo");
			String text = String.format("Dispositivo renomeado: %s → %s", oldName, newName);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "device.renamed");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("oldName", oldName);
			entry.put("newName", newName);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("facebook.activity.account_created".equals(type)
			|| "activity_account_created".equals(type)
			|| "desktop.activity.account_created".equals(type)
			|| "desktop.activity.account.created".equals(type)
			|| "desktop.activity.social_media_account_created".equals(type)) {
			String actor = account == null || account.isBlank() ? "desconhecido" : account;
			String effectivePlatform = platform == null || platform.isBlank() ? "DESCONHECIDO" : platform;
			// novo formato envia "name" em vez de profileName; mantemos compatibilidade
			String profileName = Optional.ofNullable(s(payload.get("profileName")))
				.map(String::trim).filter(v -> !v.isEmpty())
				.orElseGet(() -> Optional.ofNullable(s(payload.get("name")))
					.map(String::trim).filter(v -> !v.isEmpty()).orElse(null));
			String text = String.format("Conta cadastrada (%s)", effectivePlatform);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "account.created");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("account", actor);
			entry.put("profileName", profileName);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("facebook.activity.account_updated".equals(type)
			|| "desktop.activity.account_updated".equals(type)
			|| "desktop.activity.account.updated".equals(type)) {
			String actor = account == null || account.isBlank() ? "desconhecido" : account;
			String effectivePlatform = platform == null || platform.isBlank() ? "DESCONHECIDO" : platform;
			String profileName = Optional.ofNullable(s(payload.get("profileName"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String accountId = Optional.ofNullable(s(payload.get("accountId"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			String source = Optional.ofNullable(s(payload.get("source"))).map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
			Map<String, Object> changes = asMap(payload.get("changes"));
			String changeSummary = summarizeAccountChanges(changes);
			String text = changeSummary == null
				? String.format("Conta atualizada (%s)", effectivePlatform)
				: String.format("Conta atualizada (%s) - mudanças: %s", effectivePlatform, changeSummary);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "account.updated");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("account", actor);
			entry.put("profileName", profileName);
			entry.put("accountId", accountId);
			entry.put("source", source);
			if (changes != null && !changes.isEmpty()) {
				entry.put("changes", changes);
			}
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("desktop.activity.account_suspended".equals(type)) {
			String actor = account == null || account.isBlank() ? "desconhecido" : account;
			String effectivePlatform = platform == null || platform.isBlank() ? "DESKTOP" : platform;
			String reason = Optional.ofNullable(s(payload.get("reason"))).map(String::trim).filter(v -> !v.isEmpty()).orElse("motivo não informado");
			String text = String.format("Conta suspensa (%s) - %s", effectivePlatform, reason);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "account.suspended");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("account", actor);
			entry.put("reason", reason);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		} else if ("desktop.activity.account_blocked".equals(type)) {
			String actor = account == null || account.isBlank() ? "desconhecido" : account;
			String effectivePlatform = platform == null || platform.isBlank() ? "DESKTOP" : platform;
			String reason = Optional.ofNullable(s(payload.get("reason"))).map(String::trim).filter(v -> !v.isEmpty()).orElse("motivo não informado");
			String text = String.format("Conta bloqueada (%s) - %s", effectivePlatform, reason);
			Map<String, Object> entry = activityItem(ts, zone, actor, text);
			entry.put("type", "account.blocked");
			entry.put("eventType", type);
			entry.put("eventId", eventId);
			entry.put("platform", effectivePlatform);
			entry.put("account", actor);
			entry.put("reason", reason);
			entry.put("deviceId", deviceId);
			entry.put("ip", ip);
			return entry;
		}

		String actor = account == null || account.isBlank()
			? Optional.ofNullable(deviceId).filter(v -> !v.isBlank()).orElse("sistema")
			: account;
		String text = String.format("Evento recebido (%s)", type);
		Map<String, Object> entry = activityItem(ts, zone, actor, text);
		entry.put("type", "generic");
		entry.put("eventType", type);
		entry.put("eventId", eventId);
		entry.put("deviceId", deviceId);
		entry.put("ip", ip);
		if (!payload.isEmpty()) {
			entry.put("payload", payload);
		}
		return entry;
	}


	private <T> Optional<T> findLatest(String customerId, Class<T> type) {
		return findLatest(customerId, null, type);
	}

	private <T> Optional<T> findLatest(String customerId, String account, Class<T> type) {
		try {
			Criteria criteria = Criteria.where("customerId").is(customerId);
			if (account != null) {
				criteria = criteria.and("account").is(account);
			}
			Query q = Query.query(criteria)
				.with(Sort.by(Sort.Direction.DESC, "eventAt"))
				.limit(1);
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
	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object value) {
		if (value instanceof Map) {
			return (Map<String, Object>) value;
		}
		return null;
	}
	private String summarizeAccountChanges(Map<String, Object> changes) {
		if (changes == null || changes.isEmpty()) {
			return null;
		}
		List<String> labels = new ArrayList<>();
		if (changes.containsKey("nomePerfil")) {
			labels.add("nome do perfil");
		}
		if (changes.containsKey("password")) {
			labels.add("senha");
		}
		if (changes.containsKey("status")) {
			labels.add("status");
		}
		if (changes.containsKey("cookies")) {
			labels.add("cookies");
		}
		if (labels.isEmpty()) {
			labels = changes.keySet().stream()
				.map(String::valueOf)
				.filter(s -> s != null && !s.isBlank())
				.collect(Collectors.toList());
		}
		if (labels.isEmpty()) {
			return null;
		}
		return joinWithAnd(labels);
	}
	private String joinWithAnd(List<String> items) {
		if (items == null || items.isEmpty()) {
			return "";
		}
		if (items.size() == 1) {
			return items.get(0);
		}
		if (items.size() == 2) {
			return items.get(0) + " e " + items.get(1);
		}
		String last = items.get(items.size() - 1);
		return String.join(", ", items.subList(0, items.size() - 1)) + " e " + last;
	}
	private String s(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	private Integer i(Object v) {
		if (v == null) return null;
		if (v instanceof Number) return ((Number) v).intValue();
		try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
	}

	public Optional<Map<String, Object>> lastDeviceForAccount(String customerId, String account, String zoneId) {
		if (customerId == null || customerId.isBlank()) {
			return Optional.empty();
		}
		if (account == null || account.isBlank()) {
			return Optional.empty();
		}
		ZoneId zone = safeZone(zoneId);
		return findLatest(customerId, account, ActivitySessionStartedDocument.class)
			.map(doc -> usageInfo(Optional.ofNullable(doc.getEventAt()).orElse(doc.getReceivedAt()), zone))
			.or(() -> findLatest(customerId, account, ActivityShareBatchDocument.class)
				.map(doc -> usageInfo(Optional.ofNullable(doc.getEventAt()).orElse(doc.getReceivedAt()), zone)));
	}

	public Optional<Map<String, Object>> accountSuspensionStatus(String customerId, String account, String zoneId) {
		if (customerId == null || customerId.isBlank()) {
			return Optional.empty();
		}
		if (account == null || account.isBlank()) {
			return Optional.empty();
		}
		ZoneId zone = safeZone(zoneId);
		return findLatest(customerId, account, ActivityAccountSuspendedDocument.class)
			.map(doc -> {
				Instant startedAt = Optional.ofNullable(doc.getEventAt()).orElse(doc.getReceivedAt());
				if (startedAt == null) {
					return null;
				}
				Instant now = Instant.now();
				Duration duration = Duration.between(startedAt, now);
				Map<String, Object> data = new LinkedHashMap<>();
				data.put("suspendedSince", startedAt);
				data.put("suspendedSinceLocal", startedAt.atZone(zone).toString());
				data.put("now", now);
				data.put("nowLocal", now.atZone(zone).toString());
				long seconds = duration.getSeconds();
				data.put("durationSeconds", seconds);
				data.put("durationMinutes", seconds / 60);
				data.put("durationHours", seconds / 3600);
				data.put("reason", doc.getReason());
				return data;
			});
	}

	public Optional<Map<String, Object>> accountBlockStatus(String customerId, String account, String zoneId) {
		if (customerId == null || customerId.isBlank()) {
			return Optional.empty();
		}
		if (account == null || account.isBlank()) {
			return Optional.empty();
		}
		ZoneId zone = safeZone(zoneId);
		return findLatest(customerId, account, ActivityAccountBlockedDocument.class)
			.map(doc -> {
				Instant startedAt = Optional.ofNullable(doc.getEventAt()).orElse(doc.getReceivedAt());
				if (startedAt == null) {
					return null;
				}
				Instant now = Instant.now();
				Duration duration = Duration.between(startedAt, now);
				Map<String, Object> data = new LinkedHashMap<>();
				data.put("blockedSince", startedAt);
				data.put("blockedSinceLocal", startedAt.atZone(zone).toString());
				data.put("now", now);
				data.put("nowLocal", now.atZone(zone).toString());
				long seconds = duration.getSeconds();
				data.put("durationSeconds", seconds);
				data.put("durationMinutes", seconds / 60);
				data.put("durationHours", seconds / 3600);
				data.put("reason", doc.getReason());
				return data;
			});
	}

	private Map<String, Object> usageInfo(Instant ts, ZoneId zone) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("lastUsedAt", ts);
		data.put("lastUsedAtLocal", ts == null ? null : ts.atZone(zone).toString());
		return data;
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


