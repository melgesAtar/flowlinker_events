package com.flowlinker.events.projection.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "security_login_failed")
public class SecurityLoginFailedDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	// Novo modelo comum
	private String source; // web | device
	private String username;
	private String role;
	private String userAgent;
	private String origin; // se houver

	// Campos adicionais possíveis enviados no payload em falhas
	private String fingerprint; // se enviado
	private String appDeviceId; // deviceId vindo do app (payload)

	private String reason; // INVALID_CREDENTIALS | SERVER_UNAVAILABLE | ACCESS_DENIED | ...
	private Integer statusCode;
	private String errorCode;
	private String message;

	// Campo legado
	private String account;

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
	public String getRole() { return role; }
	public void setRole(String role) { this.role = role; }
	public String getUserAgent() { return userAgent; }
	public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
	public String getOrigin() { return origin; }
	public void setOrigin(String origin) { this.origin = origin; }
	public String getFingerprint() { return fingerprint; }
	public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
	public String getAppDeviceId() { return appDeviceId; }
	public void setAppDeviceId(String appDeviceId) { this.appDeviceId = appDeviceId; }

	public String getReason() { return reason; }
	public void setReason(String reason) { this.reason = reason; }
	public Integer getStatusCode() { return statusCode; }
	public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
	public String getErrorCode() { return errorCode; }
	public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	public String getAccount() { return account; }
	public void setAccount(String account) { this.account = account; }
}


