package com.flowlinker.events.consumer;

import com.flowlinker.events.api.dto.EnrichedEventDTO;
import com.flowlinker.events.persistence.EventDocument;
import com.flowlinker.events.persistence.EventRepository;
import com.flowlinker.events.projection.activity.*;
import com.flowlinker.events.projection.campaign.CampaignCompletedDocument;
import com.flowlinker.events.projection.campaign.CampaignCompletedRepository;
import com.flowlinker.events.projection.campaign.CampaignProgressDocument;
import com.flowlinker.events.projection.campaign.CampaignProgressRepository;
import com.flowlinker.events.projection.campaign.CampaignStartedDocument;
import com.flowlinker.events.projection.campaign.CampaignStartedRepository;
import com.flowlinker.events.projection.security.SecurityLoginFailedDocument;
import com.flowlinker.events.projection.security.SecurityLoginFailedRepository;
import com.flowlinker.events.projection.security.SecurityLoginSuccessDocument;
import com.flowlinker.events.projection.security.SecurityLoginSuccessRepository;
import com.flowlinker.events.projection.security.SecurityDeviceChangedDocument;
import com.flowlinker.events.projection.security.SecurityDeviceChangedRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionStartedRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionStartedDocument;
import com.flowlinker.events.projection.activity.ActivityExtractionPausedRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionPausedDocument;
import com.flowlinker.events.projection.activity.ActivityExtractionCancelledRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionCancelledDocument;
import com.flowlinker.events.projection.activity.ActivityExtractionCompletedRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionCompletedDocument;
import com.flowlinker.events.projection.activity.ActivityDeviceCreatedDocument;
import com.flowlinker.events.projection.activity.ActivityDeviceCreatedRepository;
import com.flowlinker.events.projection.activity.ActivityDeviceRenamedDocument;
import com.flowlinker.events.projection.activity.ActivityDeviceRenamedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.time.Instant;

@Service
public class EventConsumerService {

	private static final Logger log = LoggerFactory.getLogger(EventConsumerService.class);

	private final EventRepository eventRepository;
	private final SecurityLoginSuccessRepository securityLoginSuccessRepository;
	private final SecurityLoginFailedRepository securityLoginFailedRepository;
	private final SecurityDeviceChangedRepository securityDeviceChangedRepository;
	private final ActivitySessionStartedRepository activitySessionStartedRepository;
	private final ActivitySessionEndedRepository activitySessionEndedRepository;
	private final ActivityAccountCreatedRepository activityAccountCreatedRepository;
	private final ActivityAccountUpdatedRepository activityAccountUpdatedRepository;
	private final ActivityAccountSuspendedRepository activityAccountSuspendedRepository;
	private final ActivitySocialMediaAccountSuspendedRepository activitySocialMediaAccountSuspendedRepository;
	private final ActivityAccountBlockedRepository activityAccountBlockedRepository;
	private final ActivityShareBatchRepository activityShareBatchRepository;
	private final ActivityErrorRepository activityErrorRepository;
	private final ActivityExtractionStartedRepository activityExtractionStartedRepository;
	private final ActivityExtractionPausedRepository activityExtractionPausedRepository;
	private final ActivityExtractionCancelledRepository activityExtractionCancelledRepository;
	private final ActivityExtractionCompletedRepository activityExtractionCompletedRepository;
	private final ActivityInstagramFollowerExtractionStartedRepository activityInstagramFollowerExtractionStartedRepository;
	private final ActivityInstagramDirectMessageSentRepository activityInstagramDirectMessageSentRepository;
	private final ActivityDeviceCreatedRepository activityDeviceCreatedRepository;
	private final ActivityDeviceRenamedRepository activityDeviceRenamedRepository;
	private final CampaignStartedRepository campaignStartedRepository;
	private final CampaignProgressRepository campaignProgressRepository;
	private final CampaignCompletedRepository campaignCompletedRepository;
	private final ActivityCommentRepository activityCommentRepository;

	@Value("${app.rabbit.queue.activity}")
	private String activityQueueName;
	@Value("${app.rabbit.queue.campaign}")
	private String campaignQueueName;
	@Value("${app.rabbit.queue.security}")
	private String securityQueueName;

	public EventConsumerService(
		EventRepository eventRepository,
		SecurityLoginSuccessRepository securityLoginSuccessRepository,
		SecurityLoginFailedRepository securityLoginFailedRepository,
		SecurityDeviceChangedRepository securityDeviceChangedRepository,
		ActivitySessionStartedRepository activitySessionStartedRepository,
		ActivitySessionEndedRepository activitySessionEndedRepository,
		ActivityAccountCreatedRepository activityAccountCreatedRepository,
		ActivityAccountUpdatedRepository activityAccountUpdatedRepository,
		ActivityAccountSuspendedRepository activityAccountSuspendedRepository,
		ActivitySocialMediaAccountSuspendedRepository activitySocialMediaAccountSuspendedRepository,
		ActivityAccountBlockedRepository activityAccountBlockedRepository,
		ActivityShareBatchRepository activityShareBatchRepository,
		ActivityErrorRepository activityErrorRepository,
		ActivityExtractionStartedRepository activityExtractionStartedRepository,
		ActivityExtractionPausedRepository activityExtractionPausedRepository,
		ActivityExtractionCancelledRepository activityExtractionCancelledRepository,
		ActivityExtractionCompletedRepository activityExtractionCompletedRepository,
		ActivityInstagramFollowerExtractionStartedRepository activityInstagramFollowerExtractionStartedRepository,
		ActivityInstagramDirectMessageSentRepository activityInstagramDirectMessageSentRepository,
		ActivityDeviceCreatedRepository activityDeviceCreatedRepository,
		ActivityDeviceRenamedRepository activityDeviceRenamedRepository,
		CampaignStartedRepository campaignStartedRepository,
		CampaignProgressRepository campaignProgressRepository,
		CampaignCompletedRepository campaignCompletedRepository,
		ActivityCommentRepository activityCommentRepository
	) {
		this.eventRepository = eventRepository;
		this.securityLoginSuccessRepository = securityLoginSuccessRepository;
		this.securityLoginFailedRepository = securityLoginFailedRepository;
		this.securityDeviceChangedRepository = securityDeviceChangedRepository;
		this.activitySessionStartedRepository = activitySessionStartedRepository;
		this.activitySessionEndedRepository = activitySessionEndedRepository;
		this.activityAccountCreatedRepository = activityAccountCreatedRepository;
		this.activityAccountUpdatedRepository = activityAccountUpdatedRepository;
		this.activityAccountSuspendedRepository = activityAccountSuspendedRepository;
		this.activitySocialMediaAccountSuspendedRepository = activitySocialMediaAccountSuspendedRepository;
		this.activityAccountBlockedRepository = activityAccountBlockedRepository;
		this.activityShareBatchRepository = activityShareBatchRepository;
		this.activityErrorRepository = activityErrorRepository;
		this.activityExtractionStartedRepository = activityExtractionStartedRepository;
		this.activityExtractionPausedRepository = activityExtractionPausedRepository;
		this.activityExtractionCancelledRepository = activityExtractionCancelledRepository;
		this.activityExtractionCompletedRepository = activityExtractionCompletedRepository;
		this.activityInstagramFollowerExtractionStartedRepository = activityInstagramFollowerExtractionStartedRepository;
		this.activityInstagramDirectMessageSentRepository = activityInstagramDirectMessageSentRepository;
		this.activityDeviceCreatedRepository = activityDeviceCreatedRepository;
		this.activityDeviceRenamedRepository = activityDeviceRenamedRepository;
		this.campaignStartedRepository = campaignStartedRepository;
		this.campaignProgressRepository = campaignProgressRepository;
		this.campaignCompletedRepository = campaignCompletedRepository;
		this.activityCommentRepository = activityCommentRepository;

		log.info("RabbitMQ consumidor configurado para filas: activity='{}', campaign='{}', security='{}'",
			activityQueueName, campaignQueueName, securityQueueName);
	}

	@RabbitListener(queues = "${app.rabbit.queue.activity}")
	public void onActivity(EnrichedEventDTO event) {
		log.info("Q[activity] type={} customerId={} eventId={}", event.getEventType(), event.getCustomerId(), event.getEventId());
		handle(event);
	}

	@RabbitListener(queues = "${app.rabbit.queue.campaign}")
	public void onCampaign(EnrichedEventDTO event) {
		log.info("Q[campaign] type={} customerId={} eventId={}", event.getEventType(), event.getCustomerId(), event.getEventId());
		handle(event);
	}

	@RabbitListener(queues = "${app.rabbit.queue.security}")
	public void onSecurity(EnrichedEventDTO event) {
		log.info("Q[security] type={} customerId={} eventId={}", event.getEventType(), event.getCustomerId(), event.getEventId());
		handle(event);
	}

	private void handle(EnrichedEventDTO e) {
		// Log estruturado da solicitação de ingestão (antes de persistir/projetar)
		Map<String, Object> p0 = e.getPayload();
		String account0 = p0 == null ? null : s(p0.get("account"));
		String platform0 = p0 == null ? null : s(p0.get("platform"));
		if (platform0 == null && p0 != null) platform0 = s(p0.get("source"));
		log.debug("INGEST REQUEST: type={} customerId={} deviceId={} ip={} account={} platform={} eventId={}",
			e.getEventType(), e.getCustomerId(), e.getDeviceId(), e.getIp(), account0, platform0, e.getEventId());

		// Persiste sempre o evento bruto para trilha completa
		try {
			eventRepository.save(toDocument(e));
		} catch (DuplicateKeyException dup) {
			log.debug("Evento duplicado ignorado (eventId={}): {}", e.getEventId(), dup.getMessage());
		} catch (Exception ex) {
			log.warn("Falha ao persistir evento (eventId={}): {}", e.getEventId(), ex.getMessage());
		}
		// Projeções tipadas
		try {
			projectTyped(e);
		} catch (Exception ex) {
			log.warn("Falha ao projetar evento (eventId={}, type={}): {}", e.getEventId(), e.getEventType(), ex.getMessage());
		}
	}

	private EventDocument toDocument(EnrichedEventDTO e) {
		EventDocument doc = new EventDocument();
		doc.setEventId(e.getEventId());
		doc.setEventType(e.getEventType());
		doc.setEventAt(e.getEventAt());
		doc.setReceivedAt(e.getReceivedAt());
		doc.setPayload(e.getPayload());
		doc.setCustomerId(e.getCustomerId());
		doc.setDeviceId(e.getDeviceId());
		doc.setIp(e.getIp());
		return doc;
	}

	private void projectTyped(EnrichedEventDTO e) {
		String type = e.getEventType();
		log.debug("projectTyped chamado para eventType={}, eventId={}", type, e.getEventId());
		Map<String, Object> p = e.getPayload();
		if (p == null) {
			p = Collections.emptyMap();
		}
		// Novo: tratamento de eventos de comentário
		if ("facebook.activity.comment".equals(type) || "instagram.activity.comment".equals(type) || "desktop.activity.comment".equals(type) || (type != null && type.endsWith(".activity.comment"))) {
			ActivityCommentDocument d = new ActivityCommentDocument();
			fillMeta(d, e);
			String platform = s(p.get("platform"));
			if (platform == null) platform = s(p.get("source"));
			d.setPlatform(platform);
			d.setAccount(s(p.get("account")));
			d.setCampaignId(l(p.get("campaignId")));
			d.setCampaignName(s(p.get("campaignName")));
			Object successObj = p.get("success");
			if (successObj == null) {
				d.setSuccess(null);
			} else if (successObj instanceof Boolean) {
				d.setSuccess((Boolean) successObj);
			} else {
				d.setSuccess(Boolean.valueOf(String.valueOf(successObj)));
			}
			d.setOrderIndex(i(p.get("orderIndex")));
			d.setCommentText(s(p.get("commentText")));
			d.setErrorReason(s(p.get("errorReason")));
			// fallback: common alternative keys for error reason
			if ((d.getErrorReason() == null || d.getErrorReason().isBlank()) && p != null) {
				Object alt = p.get("error_reason");
				if (alt == null) alt = p.get("error");
				if (alt instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String,Object> em = (Map<String,Object>) alt;
					if (em.get("reason") != null) d.setErrorReason(String.valueOf(em.get("reason")));
					else if (em.get("code") != null) d.setErrorReason(String.valueOf(em.get("code")));
					else if (em.get("errorReason") != null) d.setErrorReason(String.valueOf(em.get("errorReason")));
				} else if (alt != null) {
					d.setErrorReason(String.valueOf(alt));
				} else if (p.get("errorCode") != null) {
					d.setErrorReason(String.valueOf(p.get("errorCode")));
				} else if (p.get("errorReasonCode") != null) {
					d.setErrorReason(String.valueOf(p.get("errorReasonCode")));
				}
			}
			// orderIndex and commentText already set earlier; keep errorReason from fallback above
			// set remaining fields
			d.setErrorMessage(s(p.get("errorMessage")));
			Object retry = p.get("retryable");
			if (retry != null) d.setRetryable(Boolean.valueOf(String.valueOf(retry)));
			// garante que o campo errorReason exista (persistido) mesmo quando nulo — salva como string vazia
			if (d.getErrorReason() == null) d.setErrorReason("");
			saveIgnoreDup(new SaveOp() { public void run() { activityCommentRepository.save(d); } });

			// Se sucesso e orderIndex presente, gravar um CampaignProgressDocument para refletir progresso
			if (Boolean.TRUE.equals(d.getSuccess()) && d.getOrderIndex() != null && d.getCampaignId() != null) {
				CampaignProgressDocument pd = new CampaignProgressDocument();
				pd.setEventId(e.getEventId());
				pd.setEventAt(e.getEventAt());
				pd.setReceivedAt(e.getReceivedAt());
				pd.setCustomerId(String.valueOf(e.getCustomerId()));
				pd.setDeviceId(String.valueOf(e.getDeviceId()));
				pd.setIp(e.getIp());
				pd.setPlatform(platform != null ? platform.toUpperCase() : null);
				pd.setEventType(type);
				pd.setCampaignId(d.getCampaignId());
				pd.setLastProcessedIndex(d.getOrderIndex());
				pd.setTotal(null);
				saveIgnoreDup(new SaveOp() { public void run() { campaignProgressRepository.save(pd); } });
			}
			return;
		}

		if ("security.auth.login".equals(type) || "auth.security.login".equals(type)) {
			SecurityLoginSuccessDocument d = new SecurityLoginSuccessDocument();
			fillMeta(d, e);
			String source = s(p.get("source"));
			d.setSource(source);
			d.setUsername(s(p.get("username")));
			d.setRole(s(p.get("role")));
			d.setUserAgent(s(p.get("userAgent")));
			// variante web
			if ("web".equalsIgnoreCase(source)) {
				d.setOrigin(s(p.get("origin")));
				d.setAuthTypeHeader(s(p.get("authTypeHeader")));
			}
			// variante device
			if ("device".equalsIgnoreCase(source)) {
				d.setFingerprint(s(p.get("fingerprint")));
				d.setAppDeviceId(s(p.get("deviceId")));
				d.setHwHash(s(p.get("hwHash")));
				d.setOsName(s(p.get("osName")));
				d.setOsVersion(s(p.get("osVersion")));
				d.setArch(s(p.get("arch")));
				d.setHostname(s(p.get("hostname")));
				d.setAppVersion(s(p.get("appVersion")));
				d.setStatus(s(p.get("status")));
			}
			saveIgnoreDup(new SaveOp() { public void run() { securityLoginSuccessRepository.save(d); } });
		} else if ("security.auth.login_failed".equals(type) || "auth.security.login_failed".equals(type)) {
			SecurityLoginFailedDocument d = new SecurityLoginFailedDocument();
			fillMeta(d, e);
			d.setSource(s(p.get("source")));
			d.setUsername(s(p.get("username")));
			d.setRole(s(p.get("role")));
			d.setUserAgent(s(p.get("userAgent")));
			d.setOrigin(s(p.get("origin")));
			d.setFingerprint(s(p.get("fingerprint")));
			d.setAppDeviceId(s(p.get("deviceId")));
			d.setReason(s(p.get("reason")));
			d.setMessage(s(p.get("message")));
			saveIgnoreDup(new SaveOp() { public void run() { securityLoginFailedRepository.save(d); } });
		} else if ("auth.security.device_changed".equals(type) || "security.auth.device_changed".equals(type)) {
			SecurityDeviceChangedDocument d = new SecurityDeviceChangedDocument();
			fillMeta(d, e);
			d.setSource(s(p.get("source")));
			d.setUsername(s(p.get("username")));
			d.setFingerprint(s(p.get("fingerprint")));
			d.setAppDeviceId(s(p.get("deviceId")));
			d.setBaselineHwHash(s(p.get("baselineHwHash")));
			d.setLastHwHash(s(p.get("lastHwHash")));
			d.setNewHwHash(s(p.get("newHwHash")));
			d.setDiffRatio(d(p.get("diffRatio")));
			d.setOsName(s(p.get("osName")));
			d.setOsVersion(s(p.get("osVersion")));
			d.setArch(s(p.get("arch")));
			d.setHostname(s(p.get("hostname")));
			d.setAppVersion(s(p.get("appVersion")));
			d.setStatus(s(p.get("status")));
			d.setUserAgent(s(p.get("userAgent")));
			saveIgnoreDup(new SaveOp() { public void run() { securityDeviceChangedRepository.save(d); } });
		} else if ("desktop.security.login_success".equals(type) || "desktop.security.login.success".equals(type)) {
			SecurityLoginSuccessDocument d = new SecurityLoginSuccessDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setAccount(s(p.get("account")));
			d.setFingerprint(s(p.get("fingerprint")));
			saveIgnoreDup(new SaveOp() { public void run() { securityLoginSuccessRepository.save(d); } });
		} else if ("desktop.security.login_failed".equals(type) || "desktop.security.login.failed".equals(type)) {
			SecurityLoginFailedDocument d = new SecurityLoginFailedDocument();
			fillMeta(d, e);
			d.setReason(s(p.get("reason")));
			d.setStatusCode(i(p.get("statusCode")));
			d.setErrorCode(s(p.get("errorCode")));
			d.setMessage(s(p.get("message")));
			d.setAccount(s(p.get("account")));
			saveIgnoreDup(new SaveOp() { public void run() { securityLoginFailedRepository.save(d); } });
		} else if ("desktop.activity.session_started".equals(type)) {
			ActivitySessionStartedDocument d = new ActivitySessionStartedDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setAccount(s(p.get("account")));
			saveIgnoreDup(new SaveOp() { public void run() { activitySessionStartedRepository.save(d); } });
		} else if ("desktop.activity.session_ended".equals(type) || "facebook.activity.session_ended".equals(type)) {
			ActivitySessionEndedDocument d = new ActivitySessionEndedDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setAccount(s(p.get("account")));
			d.setReason(s(p.get("reason")));
			saveIgnoreDup(new SaveOp() { public void run() { activitySessionEndedRepository.save(d); } });
		} else if ("facebook.activity.account_created".equals(type)
				|| "activity_account_created".equals(type)
				|| "desktop.activity.account_created".equals(type)
				|| "desktop.activity.account.created".equals(type)
				|| "desktop.activity.social_media_account_created".equals(type)) {
			ActivityAccountCreatedDocument d = new ActivityAccountCreatedDocument();
			fillMeta(d, e);
			// tenta platform, senão usa source para manter compatibilidade com o desktop
			String platform = s(p.get("platform"));
			if (platform == null) platform = s(p.get("source"));
			d.setPlatform(platform);
			// novo formato envia username/name, mas mantemos compatibilidade com account/profileName
			String accountValue = s(p.get("account"));
			if (accountValue == null) accountValue = s(p.get("username"));
			if (accountValue == null) accountValue = s(p.get("name"));
			d.setAccount(accountValue);
			String profileName = s(p.get("profileName"));
			if (profileName == null) profileName = s(p.get("name"));
			d.setProfileName(profileName);
			saveIgnoreDup(new SaveOp() { public void run() { activityAccountCreatedRepository.save(d); } });
		} else if ("desktop.activity.social_media_account_updated".equals(type) || "web.activity.social.media.account_updated".equals(type)) {
			ActivityAccountUpdatedDocument d = new ActivityAccountUpdatedDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setAccount(s(p.get("account")));
			d.setProfileName(s(p.get("profileName")));
			d.setAccountId(s(p.get("accountId")));
			d.setSource(s(p.get("source")));
			Object changes = p.get("changes");
			if (changes instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> mapChanges = (Map<String, Object>) changes;
				d.setChanges(mapChanges);
			} else {
				d.setChanges(null);
			}
			saveIgnoreDup(new SaveOp() { public void run() { activityAccountUpdatedRepository.save(d); } });
		} else if ("desktop.activity.social_media_account_suspended".equals(type) || "web.activity.social_media_account_suspended".equals(type)) {
			log.debug("PROCESSANDO evento de suspensão de conta de rede social: eventId={}, type={}", e.getEventId(), type);
			ActivitySocialMediaAccountSuspendedDocument d = new ActivitySocialMediaAccountSuspendedDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setAccount(s(p.get("account")));
			d.setReason(s(p.get("reason")));
			log.debug("Dados do documento antes de salvar: platform={}, account={}, reason={}, eventId={}", 
					d.getPlatform(), d.getAccount(), d.getReason(), d.getEventId());
			saveIgnoreDup(new SaveOp() { 
				public void run() { 
					activitySocialMediaAccountSuspendedRepository.save(d);
					log.debug("Evento de suspensão SALVO na collection activity_social_media_account_suspended: eventId={}", d.getEventId());
				} 
			});
		} else if ("desktop.activity.account_suspended".equals(type)) {
			ActivityAccountSuspendedDocument d = new ActivityAccountSuspendedDocument();
			fillMeta(d, e);
			String platform = s(p.get("platform"));
			if (platform == null) platform = s(p.get("source"));
			if (platform == null && type.startsWith("desktop")) {
				platform = "DESKTOP";
			}
			d.setPlatform(platform);
			d.setAccount(s(p.get("account")));
			d.setReason(s(p.get("reason")));
			saveIgnoreDup(new SaveOp() { public void run() { activityAccountSuspendedRepository.save(d); } });
		} else if ("desktop.activity.account_blocked".equals(type)) {
			ActivityAccountBlockedDocument d = new ActivityAccountBlockedDocument();
			fillMeta(d, e);
			String platform = s(p.get("platform"));
			if (platform == null) platform = s(p.get("source"));
			if (platform == null && type.startsWith("desktop")) {
				platform = "DESKTOP";
			}
			d.setPlatform(platform);
			d.setAccount(s(p.get("account")));
			d.setReason(s(p.get("reason")));
			saveIgnoreDup(new SaveOp() { public void run() { activityAccountBlockedRepository.save(d); } });
		} else if ("facebook.activity.extraction.started".equals(type)) {
			ActivityExtractionStartedDocument d = new ActivityExtractionStartedDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setKeywords(s(p.get("keywords")));
			d.setTotalGroups(l(p.get("totalGroups")));
			d.setTotalMembers(l(p.get("totalMembers")));
			saveIgnoreDup(new SaveOp() { public void run() { activityExtractionStartedRepository.save(d); } });
		} else if ("facebook.activity.extraction.paused".equals(type)) {
			ActivityExtractionPausedDocument d = new ActivityExtractionPausedDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setKeywords(s(p.get("keywords")));
			d.setTotalGroups(l(p.get("totalGroups")));
			d.setTotalMembers(l(p.get("totalMembers")));
			saveIgnoreDup(new SaveOp() { public void run() { activityExtractionPausedRepository.save(d); } });
		} else if ("facebook.activity.extraction.cancelled".equals(type)) {
			ActivityExtractionCancelledDocument d = new ActivityExtractionCancelledDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setKeywords(s(p.get("keywords")));
			d.setTotalGroups(l(p.get("totalGroups")));
			d.setTotalMembers(l(p.get("totalMembers")));
			saveIgnoreDup(new SaveOp() { public void run() { activityExtractionCancelledRepository.save(d); } });
		} else if ("facebook.activity.extraction.completed".equals(type)) {
			ActivityExtractionCompletedDocument d = new ActivityExtractionCompletedDocument();
			fillMeta(d, e);
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if ("desktop.activity.instagram_follower_extraction_started".equals(type)
				|| "web.activity.instagram_follower_extraction_started".equals(type)) {
			ActivityInstagramFollowerExtractionStartedDocument d = new ActivityInstagramFollowerExtractionStartedDocument();
			fillMeta(d, e);
			d.setExtractionId(l(p.get("extractionId")));
			d.setTargetUsername(s(p.get("targetUsername")));
			d.setFollowersLimit(l(p.get("followersLimit")));
			d.setFollowersCount(l(p.get("followersCount")));
			d.setAccountUsername(s(p.get("accountUsername")));
			d.setAccountPlatform(s(p.get("accountPlatform")));
			saveIgnoreDup(new SaveOp() { public void run() { activityInstagramFollowerExtractionStartedRepository.save(d); } });
		} else if ("device.activity.created".equals(type)) {
			ActivityDeviceCreatedDocument d = new ActivityDeviceCreatedDocument();
			fillMeta(d, e);
			d.setSource(s(p.get("source")));
			d.setFingerprint(s(p.get("fingerprint")));
			d.setAppDeviceId(s(p.get("deviceId")));
			d.setOsName(s(p.get("osName")));
			d.setOsVersion(s(p.get("osVersion")));
			d.setArch(s(p.get("arch")));
			d.setHostname(s(p.get("hostname")));
			d.setAppVersion(s(p.get("appVersion")));
			saveIgnoreDup(new SaveOp() { public void run() { activityDeviceCreatedRepository.save(d); } });
		} else if ("device.activity.renamed".equals(type)) {
			ActivityDeviceRenamedDocument d = new ActivityDeviceRenamedDocument();
			fillMeta(d, e);
			d.setSource(s(p.get("source")));
			d.setFingerprint(s(p.get("fingerprint")));
			d.setAppDeviceId(s(p.get("deviceId")));
			d.setOldName(s(p.get("oldName")));
			d.setNewName(s(p.get("newName")));
			saveIgnoreDup(new SaveOp() { public void run() { activityDeviceRenamedRepository.save(d); } });
		} else if ("facebook.activity.share_batch".equals(type) || "facebook.activity.share.batch".equals(type)) {
			ActivityShareBatchDocument d = new ActivityShareBatchDocument();
			fillMeta(d, e);
			d.setPlatform("FACEBOOK");
			d.setCount(i(p.get("count")));
			d.setAccount(s(p.get("account")));
			d.setGroupName(s(p.get("groupName")));
			d.setGroupUrl(s(p.get("groupUrl")));
			d.setGroupMembers(s(p.get("groupMembers")));
			d.setPost(s(p.get("post")));
			saveIgnoreDup(new SaveOp() { public void run() { activityShareBatchRepository.save(d); } });
		} else if ("instagram.activity.direct_message.sent".equals(type) 
				|| "instagram.direct_message.sent".equals(type)
				|| "desktop.activity.direct_message.sent".equals(type)
				|| "desktop.activity.direct.message.sent".equals(type)) {
			ActivityInstagramDirectMessageSentDocument d = new ActivityInstagramDirectMessageSentDocument();
			fillMeta(d, e);
			d.setPlatform("INSTAGRAM");
			d.setAccount(s(p.get("account")));
			d.setRecipientUsername(s(p.get("recipientUsername")));
			d.setRecipientId(s(p.get("recipientId")));
			d.setMessagePreview(s(p.get("messagePreview")));
			d.setCampaignId(l(p.get("campaignId")));
			d.setCampaignName(s(p.get("campaignName")));
			d.setSource(s(p.get("source")));
			saveIgnoreDup(new SaveOp() { public void run() { activityInstagramDirectMessageSentRepository.save(d); } });
		} else if ("facebook.activity.error".equals(type) || "desktop.activity.error".equals(type)) {
			ActivityErrorDocument d = new ActivityErrorDocument();
			fillMeta(d, e);
			d.setCode(s(p.get("code")));
			d.setMessage(s(p.get("message")));
			Object ctx = p.get("context");
			if (ctx instanceof java.util.Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) ctx;
				d.setContext(m);
			} else {
				d.setContext(null);
			}
			saveIgnoreDup(new SaveOp() { public void run() { activityErrorRepository.save(d); } });
		} else if (isCampaignEvent(type, "started")) {
			CampaignStartedDocument d = new CampaignStartedDocument();
			fillMeta(d, e);
			CampaignDescriptor descriptor = describeCampaign(type, p);
			d.setPlatform(descriptor.platform != null ? descriptor.platform.toUpperCase() : "INSTAGRAM");
			// Usa campaignType do payload se disponível, senão usa do descriptor
			String campaignType = s(p.get("campaignType"));
			if (campaignType == null || campaignType.isBlank()) {
				campaignType = descriptor.category;
			}
			d.setCampaignType(campaignType);
			d.setEventType(type);
			d.setCampaignId(l(p.get("campaignId")));
			// Mapeia followerExtractionId para extractionId quando necessário
			Long extractionId = l(p.get("extractionId"));
			if (extractionId == null) {
				extractionId = l(p.get("followerExtractionId"));
			}
			d.setExtractionId(extractionId);
			d.setTotal(i(p.get("total")));
			saveIgnoreDup(new SaveOp() { public void run() { campaignStartedRepository.save(d); } });
		} else if (isCampaignEvent(type, "progress")) {
			CampaignProgressDocument d = new CampaignProgressDocument();
			fillMeta(d, e);
			CampaignDescriptor descriptor = describeCampaign(type, p);
			d.setPlatform(descriptor.platform != null ? descriptor.platform.toUpperCase() : "INSTAGRAM");
			// Usa campaignType do payload se disponível, senão usa do descriptor
			String campaignType = s(p.get("campaignType"));
			if (campaignType == null || campaignType.isBlank()) {
				campaignType = descriptor.category;
			}
			d.setCampaignType(campaignType);
			d.setEventType(type);
			d.setCampaignId(l(p.get("campaignId")));
			d.setLastProcessedIndex(i(p.get("lastProcessedIndex")));
			d.setTotal(i(p.get("total")));
			saveIgnoreDup(new SaveOp() { public void run() { campaignProgressRepository.save(d); } });
		} else if (isCampaignEvent(type, "completed")) {
			CampaignCompletedDocument d = new CampaignCompletedDocument();
			fillMeta(d, e);
			CampaignDescriptor descriptor = describeCampaign(type, p);
			d.setPlatform(descriptor.platform != null ? descriptor.platform.toUpperCase() : "INSTAGRAM");
			// Usa campaignType do payload se disponível, senão usa do descriptor
			String campaignType = s(p.get("campaignType"));
			if (campaignType == null || campaignType.isBlank()) {
				campaignType = descriptor.category;
			}
			d.setCampaignType(campaignType);
			d.setEventType(type);
			d.setCampaignId(l(p.get("campaignId")));
			d.setLastProcessedIndex(i(p.get("lastProcessedIndex")));
			d.setTotal(i(p.get("total")));
			saveIgnoreDup(new SaveOp() { public void run() { campaignCompletedRepository.save(d); } });
		} else {
			log.debug("Sem projeção tipada para tipo={}", type);
		}
	}

	private interface SaveOp { void run(); }
	private void saveIgnoreDup(SaveOp op) {
		try { 
			op.run(); 
		} catch (DuplicateKeyException dup) { 
			log.debug("Evento duplicado ignorado: {}", dup.getMessage());
		} catch (Exception ex) {
			log.error("Erro ao salvar documento: {}", ex.getMessage(), ex);
			throw ex; // Re-lança para ser capturado pelo try-catch do projectTyped
		}
	}

	private void fillMeta(Object target, EnrichedEventDTO e) {
		// preenchimento manual em cada tipo (mais simples/explicito)
		if (target instanceof SecurityLoginSuccessDocument) {
			SecurityLoginSuccessDocument d = (SecurityLoginSuccessDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof SecurityLoginFailedDocument) {
			SecurityLoginFailedDocument d = (SecurityLoginFailedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof SecurityDeviceChangedDocument) {
			SecurityDeviceChangedDocument d = (SecurityDeviceChangedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivitySessionStartedDocument) {
			ActivitySessionStartedDocument d = (ActivitySessionStartedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivitySessionEndedDocument) {
			ActivitySessionEndedDocument d = (ActivitySessionEndedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityAccountCreatedDocument) {
			ActivityAccountCreatedDocument d = (ActivityAccountCreatedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityAccountUpdatedDocument) {
			ActivityAccountUpdatedDocument d = (ActivityAccountUpdatedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivitySocialMediaAccountSuspendedDocument) {
			ActivitySocialMediaAccountSuspendedDocument d = (ActivitySocialMediaAccountSuspendedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityAccountSuspendedDocument) {
			ActivityAccountSuspendedDocument d = (ActivityAccountSuspendedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityAccountBlockedDocument) {
			ActivityAccountBlockedDocument d = (ActivityAccountBlockedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityShareBatchDocument) {
			ActivityShareBatchDocument d = (ActivityShareBatchDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityErrorDocument) {
			ActivityErrorDocument d = (ActivityErrorDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityDeviceCreatedDocument) {
			ActivityDeviceCreatedDocument d = (ActivityDeviceCreatedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityDeviceRenamedDocument) {
			ActivityDeviceRenamedDocument d = (ActivityDeviceRenamedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityExtractionStartedDocument) {
			ActivityExtractionStartedDocument d = (ActivityExtractionStartedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityExtractionPausedDocument) {
			ActivityExtractionPausedDocument d = (ActivityExtractionPausedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityExtractionCancelledDocument) {
			ActivityExtractionCancelledDocument d = (ActivityExtractionCancelledDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityExtractionCompletedDocument) {
			ActivityExtractionCompletedDocument d = (ActivityExtractionCompletedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityInstagramFollowerExtractionStartedDocument) {
			ActivityInstagramFollowerExtractionStartedDocument d = (ActivityInstagramFollowerExtractionStartedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityInstagramDirectMessageSentDocument) {
			ActivityInstagramDirectMessageSentDocument d = (ActivityInstagramDirectMessageSentDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityCommentDocument) {
			ActivityCommentDocument d = (ActivityCommentDocument) target;
			// populate from top-level first
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
			// fallback: try payload values when top-level values are missing
			Map<String, Object> p = e.getPayload();
			if (p != null) {
				if ((d.getEventId() == null || d.getEventId().isBlank()) && p.get("eventId") != null) {
					d.setEventId(s(p.get("eventId")));
				}
				if (d.getEventAt() == null && p.get("eventAt") != null) {
					Object ev = p.get("eventAt");
					if (ev instanceof Instant) d.setEventAt((Instant) ev);
					else {
						try { d.setEventAt(Instant.parse(String.valueOf(ev))); } catch (Exception ex) { /* ignore */ }
					}
				}
				if (d.getReceivedAt() == null && p.get("receivedAt") != null) {
					Object rv = p.get("receivedAt");
					if (rv instanceof Instant) d.setReceivedAt((Instant) rv);
					else {
						try { d.setReceivedAt(Instant.parse(String.valueOf(rv))); } catch (Exception ex) { /* ignore */ }
					}
				}
				if ((d.getCustomerId() == null || d.getCustomerId().isBlank()) && p.get("customerId") != null) {
					d.setCustomerId(String.valueOf(p.get("customerId")));
				}
				if ((d.getDeviceId() == null || d.getDeviceId().isBlank()) && p.get("deviceId") != null) {
					d.setDeviceId(String.valueOf(p.get("deviceId")));
				}
				if ((d.getIp() == null || d.getIp().isBlank()) && p.get("ip") != null) {
					d.setIp(String.valueOf(p.get("ip")));
				}
			}
			// debug log to help troubleshooting missing fields
			log.debug("ActivityCommentDocument meta populated: eventId={} eventAt={} receivedAt={} customerId={} deviceId={} ip={} account={} campaignId={}",
					d.getEventId(), d.getEventAt(), d.getReceivedAt(), d.getCustomerId(), d.getDeviceId(), d.getIp(), d.getAccount(), d.getCampaignId());
		} else if (target instanceof CampaignStartedDocument) {
			CampaignStartedDocument d = (CampaignStartedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof CampaignProgressDocument) {
			CampaignProgressDocument d = (CampaignProgressDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof CampaignCompletedDocument) {
			CampaignCompletedDocument d = (CampaignCompletedDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		}
	}

	private String s(Object v) { return v == null ? null : String.valueOf(v); }
	private Integer i(Object v) {
		if (v == null) return null;
		if (v instanceof Number) return ((Number) v).intValue();
		try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
	}
	private Long l(Object v) {
		if (v == null) return null;
		if (v instanceof Number) return ((Number) v).longValue();
		try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
	}
	private Double d(Object v) {
		if (v == null) return null;
		if (v instanceof Number) return ((Number) v).doubleValue();
		try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return null; }
	}

	private boolean isCampaignEvent(String type, String suffix) {
		if (type == null || suffix == null) {
			return false;
		}
		return type.contains(".campaign.") && type.endsWith("." + suffix);
	}

	private CampaignDescriptor describeCampaign(String type, Map<String, Object> payload) {
		String platform = s(payload.get("platform"));
		if (platform == null) {
			platform = s(payload.get("source"));
		}
		// Tenta usar campaignType do payload primeiro
		String category = s(payload.get("campaignType"));
		if (category == null || category.isBlank()) {
			category = null;
			if (type != null) {
				String[] parts = type.split("\\.");
				int campaignIndex = -1;
				for (int i = 0; i < parts.length; i++) {
					if ("campaign".equals(parts[i])) {
						campaignIndex = i;
						break;
					}
				}
				if (campaignIndex >= 0) {
					if (platform == null && campaignIndex > 0) {
						platform = parts[0];
					}
					int variantStart = campaignIndex + 1;
					int variantEnd = parts.length - 1;
					if (variantStart < variantEnd) {
						category = String.join(".", Arrays.copyOfRange(parts, variantStart, variantEnd));
						if (category != null && category.isBlank()) {
							category = null;
						}
					}
				}
			}
		}
		return new CampaignDescriptor(platform, category);
	}

	private static final class CampaignDescriptor {
		private final String platform;
		private final String category;

		private CampaignDescriptor(String platform, String category) {
			this.platform = platform;
			this.category = category;
		}
	}
}

