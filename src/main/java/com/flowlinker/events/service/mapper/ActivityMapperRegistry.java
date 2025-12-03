package com.flowlinker.events.service.mapper;

import java.util.*;
import java.util.function.Predicate;

/**
 * Registry que associa tipos de eventos aos seus mapeadores.
 * Usa uma combinação de mapeamento direto (para tipos exatos) e predicados (para padrões).
 */
public final class ActivityMapperRegistry {

    private static final Map<String, ActivityMapper> EXACT_MAPPERS = new HashMap<>();
    private static final List<MapperRule> PATTERN_MAPPERS = new ArrayList<>();

    static {
        // Login Success
        registerExact("security.auth.login", ActivityMappers::loginSuccess);
        registerExact("auth.security.login", ActivityMappers::loginSuccess);
        registerExact("desktop.security.login_success", ActivityMappers::loginSuccess);
        registerExact("desktop.security.login.success", ActivityMappers::loginSuccess);

        // Login Failed
        registerExact("security.auth.login_failed", ActivityMappers::loginFailed);
        registerExact("auth.security.login_failed", ActivityMappers::loginFailed);
        registerExact("desktop.security.login_failed", ActivityMappers::loginFailed);
        registerExact("desktop.security.login.failed", ActivityMappers::loginFailed);

        // Device Changed
        registerExact("auth.security.device_changed", ActivityMappers::deviceChanged);
        registerExact("security.auth.device_changed", ActivityMappers::deviceChanged);

        // Device Created/Renamed
        registerExact("device.activity.created", ActivityMappers::deviceCreated);
        registerExact("device.activity.renamed", ActivityMappers::deviceRenamed);

        // Share
        registerExact("facebook.activity.share_batch", ActivityMappers::shareBatch);
        registerExact("facebook.activity.share.batch", ActivityMappers::shareBatch);
        
        // Instagram Direct Message
        registerExact("instagram.activity.direct_message.sent", ActivityMappers::instagramDirectMessageSent);
        registerExact("instagram.direct_message.sent", ActivityMappers::instagramDirectMessageSent);
        registerExact("desktop.activity.direct_message.sent", ActivityMappers::instagramDirectMessageSent);
        registerExact("desktop.activity.direct.message.sent", ActivityMappers::instagramDirectMessageSent);

        // Session
        registerExact("desktop.activity.session_started", ActivityMappers::sessionStarted);
        registerExact("desktop.activity.session.started", ActivityMappers::sessionStarted);
        registerExact("facebook.activity.session_started", ActivityMappers::sessionStarted);
        registerExact("desktop.activity.session_ended", ActivityMappers::sessionEnded);
        registerExact("desktop.activity.session.ended", ActivityMappers::sessionEnded);
        registerExact("facebook.activity.session_ended", ActivityMappers::sessionEnded);

        // Error
        registerExact("facebook.activity.error", ActivityMappers::activityError);
        registerExact("desktop.activity.error", ActivityMappers::activityError);

        // Extraction
        registerExact("facebook.activity.extraction.started", ActivityMappers::extractionStarted);
        registerExact("facebook.activity.extraction.paused", ActivityMappers::extractionPaused);
        registerExact("facebook.activity.extraction.cancelled", ActivityMappers::extractionCancelled);
        registerExact("facebook.activity.extraction.completed", ActivityMappers::extractionCompleted);
        
        // Instagram Follower Extraction
        registerExact("desktop.activity.instagram_follower_extraction_started", ActivityMappers::instagramFollowerExtractionStarted);
        registerExact("web.activity.instagram_follower_extraction_started", ActivityMappers::instagramFollowerExtractionStarted);

        // Account
        registerExact("desktop.activity.social_media_account_created", ActivityMappers::accountCreated);
        registerExact("desktop.activity.social_media_account_updated", ActivityMappers::accountUpdated);
        registerExact("web.social.media.account_updated", ActivityMappers::accountUpdated);
        registerExact("desktop.activity.account_suspended", ActivityMappers::accountSuspended);
        registerExact("desktop.activity.account.suspended", ActivityMappers::accountSuspended);
        registerExact("desktop.activity.social_media_account_suspended", ActivityMappers::accountSuspended);
        registerExact("web.activity.social_media_account_suspended", ActivityMappers::accountSuspended);
        registerExact("desktop.activity.account_blocked", ActivityMappers::accountBlocked);
        registerExact("desktop.activity.account.blocked", ActivityMappers::accountBlocked);

        // Campaign patterns (usando predicados para padrões dinâmicos)
        registerPattern(
                type -> type.contains(".campaign.") && type.endsWith(".started"),
                ActivityMappers::campaignStarted
        );
        registerPattern(
                type -> type.contains(".campaign.") && type.endsWith(".progress"),
                ActivityMappers::campaignProgress
        );
        registerPattern(
                type -> type.contains(".campaign.") && type.endsWith(".completed"),
                ActivityMappers::campaignCompleted
        );
        registerPattern(
                type -> type.contains(".campaign.") && type.endsWith(".cancelled"),
                ActivityMappers::campaignCancelled
        );
        registerPattern(
                type -> type.contains(".campaign.") && type.endsWith(".paused"),
                ActivityMappers::campaignPaused
        );
        registerPattern(
                type -> type.contains(".campaign.") && type.endsWith(".resumed"),
                ActivityMappers::campaignResumed
        );
    }

    private ActivityMapperRegistry() {}

    /**
     * Encontra o mapeador apropriado para o tipo de evento.
     *
     * @param eventType tipo do evento
     * @return o mapeador ou empty se não houver mapeador específico
     */
    public static Optional<ActivityMapper> findMapper(String eventType) {
        if (eventType == null) {
            return Optional.empty();
        }

        // Primeiro tenta mapeamento exato
        ActivityMapper exact = EXACT_MAPPERS.get(eventType);
        if (exact != null) {
            return Optional.of(exact);
        }

        // Depois tenta padrões
        for (MapperRule rule : PATTERN_MAPPERS) {
            if (rule.matches(eventType)) {
                return Optional.of(rule.mapper());
            }
        }

        return Optional.empty();
    }

    /**
     * Mapeia um evento usando o mapeador apropriado ou o genérico.
     *
     * @param ctx contexto do evento
     * @return mapa com os dados da atividade
     */
    public static Map<String, Object> map(EventContext ctx) {
        return findMapper(ctx.eventType())
                .map(mapper -> mapper.map(ctx))
                .orElseGet(() -> ActivityMappers.generic(ctx));
    }

    private static void registerExact(String eventType, ActivityMapper mapper) {
        EXACT_MAPPERS.put(eventType, mapper);
    }

    private static void registerPattern(Predicate<String> pattern, ActivityMapper mapper) {
        PATTERN_MAPPERS.add(new MapperRule(pattern, mapper));
    }

    private record MapperRule(Predicate<String> pattern, ActivityMapper mapper) {
        boolean matches(String eventType) {
            return pattern.test(eventType);
        }
    }
}

