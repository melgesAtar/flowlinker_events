package com.flowlinker.events.projection.activity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "activity_social_media_account_updated")
public class ActivityAccountUpdatedDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	private String platform;
	private String account;
	private String profileName;
	private String accountId;
	private String source;
	private Map<String, Object> changes;

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
	public String getProfileName() { return profileName; }
	public void setProfileName(String profileName) { this.profileName = profileName; }
	public String getAccountId() { return accountId; }
	public void setAccountId(String accountId) { this.accountId = accountId; }
	public String getSource() { return source; }
	public void setSource(String source) { this.source = source; }
	public Map<String, Object> getChanges() { return changes; }
	public void setChanges(Map<String, Object> changes) { this.changes = changes; }
}


