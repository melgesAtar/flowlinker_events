package com.flowlinker.events.service.mapper;

import com.flowlinker.events.persistence.EventDocument;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

/**
 * Contexto imutável com todos os dados necessários para mapear um evento.
 */
public record EventContext(
        String eventId,
        String eventType,
        Instant eventAt,
        String deviceId,
        String ip,
        ZoneId zone,
        PayloadExtractor payload
) {
    public static EventContext from(EventDocument event, ZoneId zone) {
        Map<String, Object> payloadMap = event.getPayload() != null ? event.getPayload() : Map.of();
        Instant ts = Optional.ofNullable(event.getEventAt()).orElse(event.getReceivedAt());
        if (ts == null) {
            ts = Instant.now();
        }

        return new EventContext(
                event.getEventId(),
                event.getEventType(),
                ts,
                event.getDeviceId(),
                event.getIp(),
                zone,
                new PayloadExtractor(payloadMap)
        );
    }

    /**
     * Extrai o identificador de conta do payload, tentando múltiplas chaves.
     */
    public String extractAccount() {
        return payload.firstNonEmpty("account", "username", "name");
    }

    /**
     * Extrai a plataforma do payload, tentando múltiplas chaves.
     */
    public String extractPlatform() {
        return payload.firstNonEmpty("platform", "source");
    }

    /**
     * Determina o ator (quem executou a ação).
     */
    public String resolveActor(String fallback) {
        String account = extractAccount();
        if (account != null && !account.isBlank()) {
            return account;
        }
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceId;
        }
        return fallback;
    }

    /**
     * Determina a plataforma efetiva com fallback.
     */
    public String resolvePlatform(String fallback) {
        String platform = extractPlatform();
        return (platform != null && !platform.isBlank()) ? platform : fallback;
    }

    /**
     * Infere a plataforma a partir do prefixo do tipo de evento.
     */
    public String inferPlatformFromType() {
        if (eventType == null) return "DESCONHECIDO";
        String[] parts = eventType.split("\\.");
        if (parts.length > 0) {
            return parts[0].toUpperCase();
        }
        return "DESCONHECIDO";
    }
}

