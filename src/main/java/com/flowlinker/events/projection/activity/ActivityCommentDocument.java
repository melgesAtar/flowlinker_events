package com.flowlinker.events.projection.activity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "activity_comment")
public class ActivityCommentDocument {

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
    private Long campaignId;
    private String campaignName;
    private Boolean success;
    private Integer orderIndex;
    private String commentText;
    private String errorReason;
    private String errorMessage;
    private Boolean retryable;
    private String  postTarget;

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
    public Long getCampaignId() { return campaignId; }
    public void setCampaignId(Long campaignId) { this.campaignId = campaignId; }
    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    public String getErrorReason() { return errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Boolean getRetryable() { return retryable; }
    public void setRetryable(Boolean retryable) { this.retryable = retryable; }
}

