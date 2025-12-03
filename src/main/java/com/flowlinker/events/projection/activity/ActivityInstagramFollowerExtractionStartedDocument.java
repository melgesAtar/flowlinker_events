package com.flowlinker.events.projection.activity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "activity_instagram_follower_extraction_started")
public class ActivityInstagramFollowerExtractionStartedDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	private Long extractionId;
	private String targetUsername;
	private Long followersLimit;
	private Long followersCount;
	private String accountUsername;
	private String accountPlatform;

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
	public Long getExtractionId() { return extractionId; }
	public void setExtractionId(Long extractionId) { this.extractionId = extractionId; }
	public String getTargetUsername() { return targetUsername; }
	public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }
	public Long getFollowersLimit() { return followersLimit; }
	public void setFollowersLimit(Long followersLimit) { this.followersLimit = followersLimit; }
	public Long getFollowersCount() { return followersCount; }
	public void setFollowersCount(Long followersCount) { this.followersCount = followersCount; }
	public String getAccountUsername() { return accountUsername; }
	public void setAccountUsername(String accountUsername) { this.accountUsername = accountUsername; }
	public String getAccountPlatform() { return accountPlatform; }
	public void setAccountPlatform(String accountPlatform) { this.accountPlatform = accountPlatform; }
}

