package com.flowlinker.events.projection.activity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "activity_device_created")
public class ActivityDeviceCreatedDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	private String source;
	private String fingerprint;
	private String appDeviceId;
	private String osName;
	private String osVersion;
	private String arch;
	private String hostname;
	private String appVersion;

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
	public String getSource() { return source; }
	public void setSource(String source) { this.source = source; }
	public String getFingerprint() { return fingerprint; }
	public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
	public String getAppDeviceId() { return appDeviceId; }
	public void setAppDeviceId(String appDeviceId) { this.appDeviceId = appDeviceId; }
	public String getOsName() { return osName; }
	public void setOsName(String osName) { this.osName = osName; }
	public String getOsVersion() { return osVersion; }
	public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
	public String getArch() { return arch; }
	public void setArch(String arch) { this.arch = arch; }
	public String getHostname() { return hostname; }
	public void setHostname(String hostname) { this.hostname = hostname; }
	public String getAppVersion() { return appVersion; }
	public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
}


