package com.flowlinker.events.consumer;

import com.flowlinker.events.api.dto.EnrichedEventDTO;
import com.flowlinker.events.config.RabbitConfig;
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
import com.flowlinker.events.projection.activity.ActivityExtractionStartedRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionStartedDocument;
import com.flowlinker.events.projection.activity.ActivityExtractionPausedRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionPausedDocument;
import com.flowlinker.events.projection.activity.ActivityExtractionCancelledRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionCancelledDocument;
import com.flowlinker.events.projection.activity.ActivityExtractionCompletedRepository;
import com.flowlinker.events.projection.activity.ActivityExtractionCompletedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EventConsumerService {

	private static final Logger log = LoggerFactory.getLogger(EventConsumerService.class);

	private final EventRepository eventRepository;
	private final SecurityLoginSuccessRepository securityLoginSuccessRepository;
	private final SecurityLoginFailedRepository securityLoginFailedRepository;
	private final ActivitySessionStartedRepository activitySessionStartedRepository;
	private final ActivitySessionEndedRepository activitySessionEndedRepository;
	private final ActivityAccountCreatedRepository activityAccountCreatedRepository;
	private final ActivityAccountUpdatedRepository activityAccountUpdatedRepository;
	private final ActivityShareBatchRepository activityShareBatchRepository;
	private final ActivityErrorRepository activityErrorRepository;
	private final ActivityExtractionStartedRepository activityExtractionStartedRepository;
	private final ActivityExtractionPausedRepository activityExtractionPausedRepository;
	private final ActivityExtractionCancelledRepository activityExtractionCancelledRepository;
	private final ActivityExtractionCompletedRepository activityExtractionCompletedRepository;
	private final CampaignStartedRepository campaignStartedRepository;
	private final CampaignProgressRepository campaignProgressRepository;
	private final CampaignCompletedRepository campaignCompletedRepository;

	public EventConsumerService(
		EventRepository eventRepository,
		SecurityLoginSuccessRepository securityLoginSuccessRepository,
		SecurityLoginFailedRepository securityLoginFailedRepository,
		ActivitySessionStartedRepository activitySessionStartedRepository,
		ActivitySessionEndedRepository activitySessionEndedRepository,
		ActivityAccountCreatedRepository activityAccountCreatedRepository,
		ActivityAccountUpdatedRepository activityAccountUpdatedRepository,
		ActivityShareBatchRepository activityShareBatchRepository,
		ActivityErrorRepository activityErrorRepository,
		ActivityExtractionStartedRepository activityExtractionStartedRepository,
		ActivityExtractionPausedRepository activityExtractionPausedRepository,
		ActivityExtractionCancelledRepository activityExtractionCancelledRepository,
		ActivityExtractionCompletedRepository activityExtractionCompletedRepository,
		CampaignStartedRepository campaignStartedRepository,
		CampaignProgressRepository campaignProgressRepository,
		CampaignCompletedRepository campaignCompletedRepository
	) {
		this.eventRepository = eventRepository;
		this.securityLoginSuccessRepository = securityLoginSuccessRepository;
		this.securityLoginFailedRepository = securityLoginFailedRepository;
		this.activitySessionStartedRepository = activitySessionStartedRepository;
		this.activitySessionEndedRepository = activitySessionEndedRepository;
		this.activityAccountCreatedRepository = activityAccountCreatedRepository;
		this.activityAccountUpdatedRepository = activityAccountUpdatedRepository;
		this.activityShareBatchRepository = activityShareBatchRepository;
		this.activityErrorRepository = activityErrorRepository;
		this.activityExtractionStartedRepository = activityExtractionStartedRepository;
		this.activityExtractionPausedRepository = activityExtractionPausedRepository;
		this.activityExtractionCancelledRepository = activityExtractionCancelledRepository;
		this.activityExtractionCompletedRepository = activityExtractionCompletedRepository;
		this.campaignStartedRepository = campaignStartedRepository;
		this.campaignProgressRepository = campaignProgressRepository;
		this.campaignCompletedRepository = campaignCompletedRepository;
	}

	@RabbitListener(queues = RabbitConfig.QUEUE_ACTIVITY)
	public void onActivity(EnrichedEventDTO event) {
		log.info("EVENTO RECEBIDO - fila=activity type={} customerId={} eventId={}",
			event.getEventType(), event.getCustomerId(), event.getEventId());
		handle(event);
	}

	@RabbitListener(queues = RabbitConfig.QUEUE_CAMPAIGN)
	public void onCampaign(EnrichedEventDTO event) {
		log.info("EVENTO RECEBIDO - fila=campaign type={} customerId={} eventId={}",
			event.getEventType(), event.getCustomerId(), event.getEventId());
		handle(event);
	}

	@RabbitListener(queues = RabbitConfig.QUEUE_SECURITY)
	public void onSecurity(EnrichedEventDTO event) {
		log.info("EVENTO RECEBIDO - fila=security type={} customerId={} eventId={}",
			event.getEventType(), event.getCustomerId(), event.getEventId());
		handle(event);
	}

	private void handle(EnrichedEventDTO e) {
		// Log estruturado da solicitação de ingestão (antes de persistir/projetar)
		Map<String, Object> p0 = e.getPayload();
		String account0 = p0 == null ? null : s(p0.get("account"));
		String platform0 = p0 == null ? null : s(p0.get("platform"));
		if (platform0 == null && p0 != null) platform0 = s(p0.get("source"));
		log.info("INGEST REQUEST: type={} customerId={} deviceId={} ip={} account={} platform={} eventId={}",
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
		Map<String, Object> p = e.getPayload();
		if ("desktop.security.login_success".equals(type)) {
			SecurityLoginSuccessDocument d = new SecurityLoginSuccessDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setAccount(s(p.get("account")));
			d.setFingerprint(s(p.get("fingerprint")));
			saveIgnoreDup(new SaveOp() { public void run() { securityLoginSuccessRepository.save(d); } });
		} else if ("desktop.security.login_failed".equals(type)) {
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
				|| "desktop.activity.account.created".equals(type)) {
			ActivityAccountCreatedDocument d = new ActivityAccountCreatedDocument();
			fillMeta(d, e);
			// tenta platform, senão usa source para manter compatibilidade com o desktop
			String platform = s(p.get("platform"));
			if (platform == null) platform = s(p.get("source"));
			d.setPlatform(platform);
			d.setAccount(s(p.get("account")));
			d.setProfileName(s(p.get("profileName")));
			saveIgnoreDup(new SaveOp() { public void run() { activityAccountCreatedRepository.save(d); } });
		} else if ("facebook.activity.account_updated".equals(type)
				|| "desktop.activity.account_updated".equals(type)
				|| "desktop.activity.account.updated".equals(type)) {
			ActivityAccountUpdatedDocument d = new ActivityAccountUpdatedDocument();
			fillMeta(d, e);
			d.setPlatform(s(p.get("platform")));
			d.setAccount(s(p.get("account")));
			d.setProfileName(s(p.get("profileName")));
			saveIgnoreDup(new SaveOp() { public void run() { activityAccountUpdatedRepository.save(d); } });
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
			d.setPlatform(s(p.get("platform")));
			d.setKeywords(s(p.get("keywords")));
			d.setTotalGroups(l(p.get("totalGroups")));
			d.setTotalMembers(l(p.get("totalMembers")));
			saveIgnoreDup(new SaveOp() { public void run() { activityExtractionCompletedRepository.save(d); } });
		} else if ("facebook.activity.share_batch".equals(type)) {
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
		} else if ("facebook.campaign.started".equals(type)) {
			CampaignStartedDocument d = new CampaignStartedDocument();
			fillMeta(d, e);
			d.setCampaignId(l(p.get("campaignId")));
			d.setExtractionId(l(p.get("extractionId")));
			d.setTotal(i(p.get("total")));
			saveIgnoreDup(new SaveOp() { public void run() { campaignStartedRepository.save(d); } });
		} else if ("facebook.campaign.progress".equals(type)) {
			CampaignProgressDocument d = new CampaignProgressDocument();
			fillMeta(d, e);
			d.setCampaignId(l(p.get("campaignId")));
			d.setLastProcessedIndex(i(p.get("lastProcessedIndex")));
			d.setTotal(i(p.get("total")));
			saveIgnoreDup(new SaveOp() { public void run() { campaignProgressRepository.save(d); } });
		} else if ("facebook.campaign.completed".equals(type)) {
			CampaignCompletedDocument d = new CampaignCompletedDocument();
			fillMeta(d, e);
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
		try { op.run(); }
		catch (DuplicateKeyException dup) { /* ok - idempotente */ }
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
		} else if (target instanceof ActivityShareBatchDocument) {
			ActivityShareBatchDocument d = (ActivityShareBatchDocument) target;
			d.setEventId(e.getEventId()); d.setEventAt(e.getEventAt()); d.setReceivedAt(e.getReceivedAt());
			d.setCustomerId(e.getCustomerId()); d.setDeviceId(e.getDeviceId()); d.setIp(e.getIp());
		} else if (target instanceof ActivityErrorDocument) {
			ActivityErrorDocument d = (ActivityErrorDocument) target;
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
}


