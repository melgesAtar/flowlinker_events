package com.flowlinker.events.api.dto;

import java.time.Instant;
import java.util.Map;

public class EnrichedEventDTO {
	private String eventId;
	private String eventType;
	private Instant eventAt;
	private Instant receivedAt;
	private Map<String, Object> payload;
	private String customerId;
	private String deviceId;
	private String ip;

	public EnrichedEventDTO(
		String eventId,
		String eventType,
		Instant eventAt,
		Instant receivedAt,
		Map<String, Object> payload,
		String customerId,
		String deviceId,
		String ip
	) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.eventAt = eventAt;
		this.receivedAt = receivedAt;
		this.payload = payload;
		this.customerId = customerId;
		this.deviceId = deviceId;
		this.ip = ip;
	}

	public EnrichedEventDTO() {
		// Jackson default constructor
	}

	public String getEventId() { return eventId; }
	public void setEventId(String eventId) { this.eventId = eventId; }
	public String getEventType() { return eventType; }
	public void setEventType(String eventType) { this.eventType = eventType; }
	public Instant getEventAt() { return eventAt; }
	public void setEventAt(Instant eventAt) { this.eventAt = eventAt; }
	public Instant getReceivedAt() { return receivedAt; }
	public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
	public Map<String, Object> getPayload() { return payload; }
	public void setPayload(Map<String, Object> payload) { this.payload = payload; }
	public String getCustomerId() { return customerId; }
	public void setCustomerId(String customerId) { this.customerId = customerId; }
	public String getDeviceId() { return deviceId; }
	public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }
}

