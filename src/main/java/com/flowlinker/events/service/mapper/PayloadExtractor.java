package com.flowlinker.events.service.mapper;

import java.util.Map;
import java.util.Optional;

/**
 * Utilitário para extrair valores do payload de eventos de forma segura e consistente.
 */
public final class PayloadExtractor {

    private final Map<String, Object> payload;

    public PayloadExtractor(Map<String, Object> payload) {
        this.payload = payload != null ? payload : Map.of();
    }

    public String string(String key) {
        return Optional.ofNullable(payload.get(key))
                .map(String::valueOf)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    public String stringOr(String key, String defaultValue) {
        String value = string(key);
        return value != null ? value : defaultValue;
    }

    public String firstNonEmpty(String... keys) {
        for (String key : keys) {
            String value = string(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public Long asLong(String key) {
        Object v = payload.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v).replaceAll("\\D", ""));
        } catch (Exception e) {
            return null;
        }
    }

    public long longOr(String key, long defaultValue) {
        Long value = asLong(key);
        return value != null ? value : defaultValue;
    }

    public Integer asInt(String key) {
        Object v = payload.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap(String key) {
        Object value = payload.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    public Map<String, Object> raw() {
        return payload;
    }

    public boolean isEmpty() {
        return payload.isEmpty();
    }
}

