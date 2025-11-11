package com.flowlinker.events.projection.activity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "activity_share_batch")
public class ActivityShareBatchDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	private String platform; // FACEBOOK | INSTAGRAM (futuro)
	private Integer count; // =1
	private String account;
	private String groupName;
	private String groupUrl;
	private String groupMembers; // pode vir number|string → normalizamos como string
	private String post;

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
	public Integer getCount() { return count; }
	public void setCount(Integer count) { this.count = count; }
	public String getAccount() { return account; }
	public void setAccount(String account) { this.account = account; }
	public String getGroupName() { return groupName; }
	public void setGroupName(String groupName) { this.groupName = groupName; }
	public String getGroupUrl() { return groupUrl; }
	public void setGroupUrl(String groupUrl) { this.groupUrl = groupUrl; }
	public String getGroupMembers() { return groupMembers; }
	public void setGroupMembers(String groupMembers) { this.groupMembers = groupMembers; }
	public String getPost() { return post; }
	public void setPost(String post) { this.post = post; }
}


