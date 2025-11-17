package com.flowlinker.events.projection.activity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "activity_account_suspended")
public class ActivityAccountSuspendedDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	private String platform; // DESKTOP | FACEBOOK
	private String account;
	private String reason; // TIMEOUT | LIMIT | OTHER

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getEventId() { return eventId; }
	public void setEventId(String eventId) { this.eventId = eventId; }
	public Instant getEventAt() { return eventAt; }
	public void setEventAt(Instant eventAt) { this.eventAt = eventAt; }
	public Instant getReceivedAt() { return receivedAt; }
	public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
	public String getCustomerId() { return customerId; }
	public void setCustomerId(String customerId) { this.customerId = customerId; }
	public String getDeviceId() { return deviceId; }
	public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
	public String getIp() { return ip; }
	public void setIp(String ip) { this.ip = ip; }
	public String getPlatform() { return platform; }
	public void setPlatform(String platform) { this.platform = platform; }
	public String getAccount() { return account; }
	public void setAccount(String account) { this.account = account; }
	public String getReason() { return reason; }
	public void setReason(String reason) { this.reason = reason; }
}



