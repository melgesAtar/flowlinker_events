package com.flowlinker.events.service.mapper;

import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder fluente para criar entradas de atividade.
 */
public class ActivityEntry {

    private final Map<String, Object> data = new LinkedHashMap<>();

    private ActivityEntry() {}

    public static ActivityEntry create(Instant eventAt, ZoneId zone, String actor, String text) {
        ActivityEntry entry = new ActivityEntry();
        entry.data.put("eventAt", eventAt);
        entry.data.put("eventAtLocal", eventAt.atZone(zone).toString());
        entry.data.put("zone", zone.getId());
        entry.data.put("actor", actor);
        entry.data.put("text", text);
        return entry;
    }

    public ActivityEntry type(String type) {
        data.put("type", type);
        return this;
    }

    public ActivityEntry eventType(String eventType) {
        data.put("eventType", eventType);
        return this;
    }

    public ActivityEntry eventId(String eventId) {
        data.put("eventId", eventId);
        return this;
    }

    public ActivityEntry platform(String platform) {
        data.put("platform", platform);
        return this;
    }

    public ActivityEntry put(String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
        return this;
    }

    public ActivityEntry putIfNotEmpty(String key, Map<String, Object> map) {
        if (map != null && !map.isEmpty()) {
            data.put(key, map);
        }
        return this;
    }

    public ActivityEntry deviceId(String deviceId) {
        data.put("deviceId", deviceId);
        return this;
    }

    public ActivityEntry ip(String ip) {
        data.put("ip", ip);
        return this;
    }

    public Map<String, Object> build() {
        return data;
    }
}

