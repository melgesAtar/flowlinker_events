package com.flowlinker.events.service.mapper;

import java.util.*;

/**
 * Coleção de mapeadores para diferentes tipos de eventos de atividade.
 */
public final class ActivityMappers {

    private ActivityMappers() {}

    // ========== LOGIN ==========

    public static Map<String, Object> loginSuccess(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String username = p.string("username");
        String account = p.firstNonEmpty("account", "username");
        String actor = username != null ? username
                : (account != null ? account
                : (ctx.deviceId() != null && !ctx.deviceId().isBlank() ? ctx.deviceId() : "desconhecido"));

        String source = p.string("source");
        String platform = resolvePlatformForLogin(ctx, source);
        String deviceName = p.firstNonEmpty("deviceName", "nameDevice", "hostname");

        String text;
        if ("web".equalsIgnoreCase(source)) {
            String ipUser = ctx.ip() != null && !ctx.ip().isBlank() ? ctx.ip() : "IP desconhecido";
            text = String.format("Login realizado com sucesso no painel web IP: %s", ipUser);
        } else {
            String name = deviceName != null ? deviceName
                    : (ctx.deviceId() != null && !ctx.deviceId().isBlank() ? ctx.deviceId() : "dispositivo");
            text = String.format("Login realizado com sucesso no dispositivo: %s", name);
        }

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("login.success")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("username", username)
                .put("account", account)
                .put("source", source)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    public static Map<String, Object> loginFailed(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String username = p.string("username");
        String account = p.firstNonEmpty("account", "username");
        String actor = username != null ? username
                : (account != null ? account
                : (ctx.deviceId() != null && !ctx.deviceId().isBlank() ? ctx.deviceId() : "desconhecido"));

        String source = p.string("source");
        String reason = p.string("reason");
        String message = p.string("message");
        String reasonLabel = translateLoginFailReason(message, reason);
        String deviceName = p.firstNonEmpty("deviceName", "nameDevice", "hostname");

        String text;
        if ("web".equalsIgnoreCase(source)) {
            String ipUser = ctx.ip() != null && !ctx.ip().isBlank() ? ctx.ip() : "IP desconhecido";
            text = String.format("Falha no login feito no painel web IP: %s, motivo: %s", ipUser, reasonLabel);
        } else {
            String name = deviceName != null ? deviceName
                    : (ctx.deviceId() != null && !ctx.deviceId().isBlank() ? ctx.deviceId() : "dispositivo");
            text = String.format("Falha no login feito no dispositivo: %s, motivo: %s", name, reasonLabel);
        }

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("login.failed")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .put("username", username)
                .put("account", account)
                .put("reason", reason)
                .put("message", message)
                .put("source", source)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    // ========== DEVICE ==========

    public static Map<String, Object> deviceChanged(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String username = p.string("username");
        String actor = username != null ? username
                : (ctx.deviceId() != null && !ctx.deviceId().isBlank() ? ctx.deviceId() : "desconhecido");

        String source = p.string("source");
        String platform = ctx.resolvePlatform(null);
        if (platform == null || platform.isBlank()) {
            if ("device".equalsIgnoreCase(source)) platform = "DEVICE";
            else if ("web".equalsIgnoreCase(source)) platform = "WEB";
            else platform = "DESCONHECIDO";
        }

        String text = String.format("Mudança de dispositivo detectada (%s)", platform);

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("device.changed")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("username", username)
                .put("source", source)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    public static Map<String, Object> deviceCreated(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String fingerprint = p.string("fingerprint");
        String osName = p.string("osName");
        String appVersion = p.string("appVersion");

        String actor = ctx.deviceId() != null && !ctx.deviceId().isBlank()
                ? ctx.deviceId()
                : (fingerprint != null ? fingerprint : "dispositivo");

        StringBuilder sb = new StringBuilder("Novo dispositivo registrado");
        if (osName != null || appVersion != null) {
            sb.append(" (");
            if (osName != null) sb.append(osName);
            if (appVersion != null) {
                if (osName != null) sb.append(" - ");
                sb.append("versão ").append(appVersion);
            }
            sb.append(")");
        }

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, sb.toString())
                .type("device.created")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .put("fingerprint", fingerprint)
                .put("osName", osName)
                .put("appVersion", appVersion)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    public static Map<String, Object> deviceRenamed(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String oldName = p.stringOr("oldName", "nome anterior");
        String newName = p.stringOr("newName", "novo nome");
        String actor = ctx.deviceId() != null && !ctx.deviceId().isBlank() ? ctx.deviceId() : "dispositivo";

        String text = String.format("Dispositivo renomeado: %s → %s", oldName, newName);

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("device.renamed")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .put("oldName", oldName)
                .put("newName", newName)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    // ========== CAMPAIGN ==========

    public static Map<String, Object> campaignStarted(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String actor = ctx.resolveActor("sistema");
        String platform = ctx.resolvePlatform(ctx.inferPlatformFromType());

        Long campaignId = p.asLong("campaignId");
        Integer total = p.asInt("total");
        String campaignName = p.firstNonEmpty("name", "campaignName", "title");
        String campaignType = p.firstNonEmpty("campaignType", "type", "category");

        String nameLabel = (campaignName != null && !campaignName.isBlank()) ? "\"" + campaignName + "\"" : "\"\"";
        String typeLabel = (campaignType != null && !campaignType.isBlank()) ? "\"" + campaignType + "\"" : "\"\"";

        String text;
        if (total != null) {
            text = String.format("Campanha %s iniciada - tipo de campanha: %s - %d itens previstos", nameLabel, typeLabel, total);
        } else {
            text = String.format("Campanha %s iniciada - tipo de campanha: %s", nameLabel, typeLabel);
        }

        ActivityEntry entry = ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("campaign.started")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("campaignName", campaignName)
                .put("campaignType", campaignType)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip());

        if (campaignId != null) entry.put("campaignId", campaignId);
        if (total != null) entry.put("total", total);

        return entry.build();
    }

    public static Map<String, Object> campaignProgress(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String actor = ctx.resolveActor("sistema");
        String platform = ctx.resolvePlatform(ctx.inferPlatformFromType());

        Long campaignId = p.asLong("campaignId");
        Long lastIndex = p.asLong("lastProcessedIndex");
        Integer total = p.asInt("total");

        String text;
        if (lastIndex != null && total != null && total > 0) {
            text = String.format("Campanha em andamento (%s) - %d de %d itens", platform, lastIndex, total);
        } else if (lastIndex != null) {
            text = String.format("Campanha em andamento (%s) - %d itens processados", platform, lastIndex);
        } else {
            text = String.format("Campanha em andamento (%s)", platform);
        }

        ActivityEntry entry = ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("campaign.progress")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip());

        if (campaignId != null) entry.put("campaignId", campaignId);
        if (lastIndex != null) entry.put("lastProcessedIndex", lastIndex);
        if (total != null) entry.put("total", total);

        return entry.build();
    }

    public static Map<String, Object> campaignCompleted(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String actor = ctx.resolveActor("sistema");
        String platform = ctx.resolvePlatform(ctx.inferPlatformFromType());

        Long campaignId = p.asLong("campaignId");
        Long lastIndex = p.asLong("lastProcessedIndex");
        Integer total = p.asInt("total");
        String campaignName = p.firstNonEmpty("name", "campaignName", "title");

        String nameLabel = (campaignName != null && !campaignName.isBlank()) ? "\"" + campaignName + "\" " : "";
        String text;
        if (lastIndex != null && total != null && total > 0) {
            text = String.format("Campanha %sconcluída (%s) - %d de %d itens", nameLabel, platform, lastIndex, total);
        } else if (lastIndex != null) {
            text = String.format("Campanha %sconcluída (%s) - %d itens processados", nameLabel, platform, lastIndex);
        } else {
            text = String.format("Campanha %sconcluída (%s)", nameLabel, platform);
        }

        ActivityEntry entry = ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("campaign.completed")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("campaignName", campaignName)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip());

        if (campaignId != null) entry.put("campaignId", campaignId);
        if (lastIndex != null) entry.put("lastProcessedIndex", lastIndex);
        if (total != null) entry.put("total", total);

        return entry.build();
    }

    public static Map<String, Object> campaignCancelled(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String actor = ctx.resolveActor("sistema");
        String platform = ctx.resolvePlatform(ctx.inferPlatformFromType());

        Long campaignId = p.asLong("campaignId");
        Long lastIndex = p.asLong("lastProcessedIndex");
        Integer total = p.asInt("total");
        String reason = p.string("reason");
        String campaignName = p.firstNonEmpty("name", "campaignName", "title");

        String nameLabel = (campaignName != null && !campaignName.isBlank()) ? "\"" + campaignName + "\" " : "";
        String text;
        if (lastIndex != null && total != null && total > 0) {
            text = String.format("Campanha %scancelada (%s) - %d de %d itens", nameLabel, platform, lastIndex, total);
        } else if (lastIndex != null) {
            text = String.format("Campanha %scancelada (%s) - %d itens processados", nameLabel, platform, lastIndex);
        } else {
            text = String.format("Campanha %scancelada (%s)", nameLabel, platform);
        }

        if (reason != null && !reason.isBlank()) {
            text += " - " + reason;
        }

        ActivityEntry entry = ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("campaign.cancelled")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("campaignName", campaignName)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip());

        if (campaignId != null) entry.put("campaignId", campaignId);
        if (lastIndex != null) entry.put("lastProcessedIndex", lastIndex);
        if (total != null) entry.put("total", total);
        if (reason != null) entry.put("reason", reason);

        return entry.build();
    }

    public static Map<String, Object> campaignPaused(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String actor = ctx.resolveActor("sistema");
        String platform = ctx.resolvePlatform(ctx.inferPlatformFromType());

        Long campaignId = p.asLong("campaignId");
        Long lastIndex = p.asLong("lastProcessedIndex");
        Integer total = p.asInt("total");
        String reason = p.string("reason");
        String campaignName = p.firstNonEmpty("name", "campaignName", "title");

        String nameLabel = (campaignName != null && !campaignName.isBlank()) ? "\"" + campaignName + "\" " : "";
        String text;
        if (lastIndex != null && total != null && total > 0) {
            text = String.format("Campanha %spausada (%s) - %d de %d itens", nameLabel, platform, lastIndex, total);
        } else if (lastIndex != null) {
            text = String.format("Campanha %spausada (%s) - %d itens processados", nameLabel, platform, lastIndex);
        } else {
            text = String.format("Campanha %spausada (%s)", nameLabel, platform);
        }

        if (reason != null && !reason.isBlank()) {
            text += " - " + reason;
        }

        ActivityEntry entry = ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("campaign.paused")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("campaignName", campaignName)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip());

        if (campaignId != null) entry.put("campaignId", campaignId);
        if (lastIndex != null) entry.put("lastProcessedIndex", lastIndex);
        if (total != null) entry.put("total", total);
        if (reason != null) entry.put("reason", reason);

        return entry.build();
    }

    // ========== SHARE ==========

    public static Map<String, Object> shareBatch(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String account = ctx.extractAccount();
        String actor = (account != null && !account.isBlank()) ? account : "";
        String platform = ctx.resolvePlatform("FACEBOOK");

        String groupName = p.string("groupName");
        String groupNameLabel = (groupName != null && !groupName.isBlank()) ? "\"" + groupName + "\"" : "\"\"";
        String accountLabel = (actor != null && !actor.isBlank()) ? "\"" + actor + "\"" : "\"\"";
        
        String text = String.format("Compartilhamento no grupo %s pela conta %s", groupNameLabel, accountLabel);

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor != null && !actor.isBlank() ? actor : "desconhecido", text)
                .type("share")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("account", actor)
                .put("groupName", groupName)
                .put("groupUrl", p.string("groupUrl"))
                .put("post", p.string("post"))
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    // ========== SESSION ==========

    public static Map<String, Object> sessionStarted(EventContext ctx) {
        String account = ctx.extractAccount();
        String actor = (account != null && !account.isBlank()) ? account : ctx.resolveActor("desconhecido");

        String platform = ctx.resolvePlatform(
                ctx.eventType().startsWith("desktop") ? "DESKTOP" : "FACEBOOK");

        String text = String.format("Sessão iniciada: %s (%s)", actor, platform);

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("session.started")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("account", actor)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    public static Map<String, Object> sessionEnded(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String account = ctx.extractAccount();
        String actor = (account != null && !account.isBlank()) ? account : ctx.resolveActor("desconhecido");

        String platform = ctx.resolvePlatform(
                ctx.eventType().startsWith("desktop") ? "DESKTOP" : "FACEBOOK");
        String reason = p.stringOr("reason", "motivo desconhecido");

        String text = String.format("Sessão encerrada: %s (%s) - %s", actor, platform, reason);

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("session.ended")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("account", actor)
                .put("reason", reason)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    // ========== ERROR ==========

    public static Map<String, Object> activityError(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String actor = ctx.resolveActor("sistema");

        String code = p.stringOr("code", "desconhecido");
        String message = p.string("message");

        String text = message == null
                ? String.format("Erro reportado (%s)", code)
                : String.format("Erro reportado (%s): %s", code, message);

        ActivityEntry entry = ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("error")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .put("code", code)
                .put("message", message)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip());

        Map<String, Object> context = p.asMap("context");
        if (context != null) {
            entry.put("context", context);
        }

        return entry.build();
    }

    // ========== EXTRACTION ==========

    public static Map<String, Object> extractionStarted(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String keywords = p.stringOr("keywords", "");
        long totalGroups = p.longOr("totalGroups", 0);
        long totalMembers = p.longOr("totalMembers", 0);

        String text = String.format("Extração iniciada - keywords: %s", keywords);

        return createExtractionEntry(ctx, "extraction.started", text, totalGroups, totalMembers);
    }

    public static Map<String, Object> extractionPaused(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        long totalGroups = p.longOr("totalGroups", 0);
        long totalMembers = p.longOr("totalMembers", 0);

        String text = String.format("Extração pausada - %d grupos, %d pessoas", totalGroups, totalMembers);

        return createExtractionEntry(ctx, "extraction.paused", text, totalGroups, totalMembers);
    }

    public static Map<String, Object> extractionCancelled(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        long totalGroups = p.longOr("totalGroups", 0);
        long totalMembers = p.longOr("totalMembers", 0);

        String text = String.format("Extração cancelada - %d grupos processados", totalGroups);

        return createExtractionEntry(ctx, "extraction.cancelled", text, totalGroups, totalMembers);
    }

    public static Map<String, Object> extractionCompleted(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        long totalGroups = p.longOr("totalGroups", 0);
        long totalMembers = p.longOr("totalMembers", 0);

        String text = String.format("Extração concluída - %d grupos, %d pessoas", totalGroups, totalMembers);

        return createExtractionEntry(ctx, "extraction.completed", text, totalGroups, totalMembers);
    }

    // ========== ACCOUNT ==========

    public static Map<String, Object> accountCreated(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String account = ctx.extractAccount();
        String actor = (account != null && !account.isBlank()) ? account : "desconhecido";
        String platform = ctx.resolvePlatform("DESCONHECIDO");
        String profileName = p.firstNonEmpty("profileName", "name");

        String text = String.format("Conta cadastrada: %s (%s)", actor, platform);

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("account.created")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("account", actor)
                .put("profileName", profileName)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    public static Map<String, Object> accountUpdated(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String account = ctx.extractAccount();
        String actor = (account != null && !account.isBlank()) ? account : "desconhecido";
        String platform = ctx.resolvePlatform("DESCONHECIDO");

        String profileName = p.string("profileName");
        String accountId = p.string("accountId");
        String source = p.string("source");
        Map<String, Object> changes = p.asMap("changes");

        String changeSummary = summarizeChanges(changes);
        String text = changeSummary == null
                ? String.format("Conta atualizada: %s (%s)", actor, platform)
                : String.format("Conta atualizada: %s (%s) - mudanças: %s", actor, platform, changeSummary);

        ActivityEntry entry = ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("account.updated")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("account", actor)
                .put("profileName", profileName)
                .put("accountId", accountId)
                .put("source", source)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip());

        entry.putIfNotEmpty("changes", changes);

        return entry.build();
    }

    public static Map<String, Object> accountSuspended(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String account = ctx.extractAccount();
        String actor = (account != null && !account.isBlank()) ? account : "desconhecido";
        String platform = ctx.resolvePlatform("DESKTOP");
        String reason = p.stringOr("reason", "motivo não informado");

        String text = String.format("Conta suspensa: %s (%s) - %s", actor, platform, reason);

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("account.suspended")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("account", actor)
                .put("reason", reason)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    public static Map<String, Object> accountBlocked(EventContext ctx) {
        PayloadExtractor p = ctx.payload();
        String account = ctx.extractAccount();
        String actor = (account != null && !account.isBlank()) ? account : "desconhecido";
        String platform = ctx.resolvePlatform("DESKTOP");
        String reason = p.stringOr("reason", "motivo não informado");

        String text = String.format("Conta bloqueada: %s (%s) - %s", actor, platform, reason);

        return ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("account.blocked")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .platform(platform)
                .put("account", actor)
                .put("reason", reason)
                .deviceId(ctx.deviceId())
                .ip(ctx.ip())
                .build();
    }

    // ========== GENERIC ==========

    public static Map<String, Object> generic(EventContext ctx) {
        String actor = ctx.resolveActor("sistema");
        String text = String.format("Evento recebido (%s)", ctx.eventType());

        ActivityEntry entry = ActivityEntry.create(ctx.eventAt(), ctx.zone(), actor, text)
                .type("generic")
                .eventType(ctx.eventType())
                .eventId(ctx.eventId())
                .deviceId(ctx.deviceId())
                .ip(ctx.ip());

        if (!ctx.payload().isEmpty()) {
            entry.put("payload", ctx.payload().raw());
        }

        return entry.build();
    }

    // ========== HELPERS ==========

    private static Map<String, Object> createExtractionEntry(EventContext ctx, String type, String text,
                                                              long totalGroups, long totalMembers) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", type);
        entry.put("eventAt", ctx.eventAt());
        entry.put("eventAtLocal", ctx.eventAt().atZone(ctx.zone()).toString());
        entry.put("zone", ctx.zone().getId());
        entry.put("text", text);
        entry.put("totalGroups", totalGroups);
        entry.put("totalMembers", totalMembers);
        entry.put("eventType", ctx.eventType());
        entry.put("eventId", ctx.eventId());
        entry.put("deviceId", ctx.deviceId());
        entry.put("ip", ctx.ip());
        return entry;
    }

    private static String resolvePlatformForLogin(EventContext ctx, String source) {
        String platform = ctx.extractPlatform();
        if (platform != null && !platform.isBlank()) {
            return platform;
        }
        if (ctx.eventType().startsWith("desktop")) return "DESKTOP";
        if (ctx.eventType().startsWith("facebook")) return "FACEBOOK";
        if ("device".equalsIgnoreCase(source)) return "DEVICE";
        if ("web".equalsIgnoreCase(source)) return "WEB";
        return "DESCONHECIDO";
    }

    private static String translateLoginFailReason(String message, String reason) {
        if (message != null) return message;
        if (reason == null) return "motivo desconhecido";

        return switch (reason.toLowerCase(Locale.ROOT)) {
            case "invalid_credentials", "invalidcredential" -> "credenciais inválidas";
            case "access_denied" -> "acesso negado";
            case "server_unavailable", "service_unavailable" -> "servidor indisponível";
            case "account_locked" -> "conta bloqueada";
            case "account_disabled" -> "conta desativada";
            default -> reason;
        };
    }

    private static String summarizeChanges(Map<String, Object> changes) {
        if (changes == null || changes.isEmpty()) return null;

        List<String> labels = new ArrayList<>();
        if (changes.containsKey("nomePerfil") || changes.containsKey("profileName")) labels.add("nome do perfil");
        if (changes.containsKey("password")) labels.add("senha");
        if (changes.containsKey("status")) labels.add("status");
        if (changes.containsKey("cookies")) labels.add("cookies");
        if (changes.containsKey("account")) labels.add("conta");

        if (labels.isEmpty()) {
            labels.addAll(changes.keySet().stream()
                    .filter(s -> s != null && !s.isBlank())
                    .toList());
        }

        if (labels.isEmpty()) return null;

        return joinWithAnd(labels);
    }

    private static String joinWithAnd(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        if (items.size() == 2) return items.get(0) + " e " + items.get(1);
        return String.join(", ", items.subList(0, items.size() - 1)) + " e " + items.get(items.size() - 1);
    }
}

