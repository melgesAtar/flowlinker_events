package com.flowlinker.events.projection.activity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "activity_instagram_direct_message_sent")
public class ActivityInstagramDirectMessageSentDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	private String platform; // INSTAGRAM
	private String account; // Conta que está enviando
	private String recipientUsername; // Username do destinatário
	private String recipientId; // ID do destinatário
	private String messagePreview; // Preview da mensagem
	private Long campaignId; // ID da campanha (opcional)
	private String campaignName; // Nome da campanha (opcional)
	private String source; // DESKTOP ou WEB

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
	public String getRecipientUsername() { return recipientUsername; }
	public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }
	public String getRecipientId() { return recipientId; }
	public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
	public String getMessagePreview() { return messagePreview; }
	public void setMessagePreview(String messagePreview) { this.messagePreview = messagePreview; }
	public Long getCampaignId() { return campaignId; }
	public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
	public String getCampaignName() { return campaignName; }
	public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
	public String getSource() { return source; }
	public void setSource(String source) { this.source = source; }
}

