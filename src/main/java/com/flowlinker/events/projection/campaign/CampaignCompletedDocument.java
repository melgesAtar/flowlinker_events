package com.flowlinker.events.projection.campaign;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "campaign_completed")
public class CampaignCompletedDocument {

	@Id
	private String id;

	@Indexed(unique = true)
	private String eventId;

	private Instant eventAt;
	private Instant receivedAt;
	private String customerId;
	private String deviceId;
	private String ip;

	private Long campaignId;
	private Integer lastProcessedIndex;
	private Integer total;

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
	public Long getCampaignId() { return campaignId; }
	public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
	public Integer getLastProcessedIndex() { return lastProcessedIndex; }
	public void setLastProcessedIndex(Integer lastProcessedIndex) { this.lastProcessedIndex = lastProcessedIndex; }
	public Integer getTotal() { return total; }
	public void setTotal(Integer total) { this.total = total; }
}


