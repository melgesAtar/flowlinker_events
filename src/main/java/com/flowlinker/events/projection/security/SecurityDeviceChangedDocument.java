package com.flowlinker.events.projection.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "security_device_changed")
public class SecurityDeviceChangedDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	private String source; // esperado "device"
	private String username;
	private String fingerprint;
	private String appDeviceId; // deviceId vindo do app no payload
	private String baselineHwHash;
	private String lastHwHash;
	private String newHwHash;
	private Double diffRatio;
	private String osName;
	private String osVersion;
	private String arch;
	private String hostname;
	private String appVersion;
	private String status;
	private String userAgent;

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

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }

	public String getFingerprint() { return fingerprint; }
	public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

	public String getAppDeviceId() { return appDeviceId; }
	public void setAppDeviceId(String appDeviceId) { this.appDeviceId = appDeviceId; }

	public String getBaselineHwHash() { return baselineHwHash; }
	public void setBaselineHwHash(String baselineHwHash) { this.baselineHwHash = baselineHwHash; }

	public String getLastHwHash() { return lastHwHash; }
	public void setLastHwHash(String lastHwHash) { this.lastHwHash = lastHwHash; }

	public String getNewHwHash() { return newHwHash; }
	public void setNewHwHash(String newHwHash) { this.newHwHash = newHwHash; }

	public Double getDiffRatio() { return diffRatio; }
	public void setDiffRatio(Double diffRatio) { this.diffRatio = diffRatio; }

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

	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	public String getUserAgent() { return userAgent; }
	public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}


