package com.flowlinker.events.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import com.flowlinker.events.api.dto.EnrichedEventDTO;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitConfig {

	public static final String EXCHANGE_EVENTS = "events.exchange";
	public static final String QUEUE_ACTIVITY = "events.activity";
	public static final String QUEUE_CAMPAIGN = "events.campaign";
	public static final String QUEUE_SECURITY = "events.security";

	@Bean
	public TopicExchange eventsExchange() {
		return new TopicExchange(EXCHANGE_EVENTS, true, false);
	}

	@Bean
	public Queue activityQueue() {
		return new Queue(QUEUE_ACTIVITY, true);
	}

	@Bean
	public Queue campaignQueue() {
		return new Queue(QUEUE_CAMPAIGN, true);
	}

	@Bean
	public Queue securityQueue() {
		return new Queue(QUEUE_SECURITY, true);
	}

	@Bean
	public Binding bindActivityDesktop(@Qualifier("activityQueue") Queue activityQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(activityQueue).to(eventsExchange).with("desktop.activity.*");
	}

	@Bean
	public Binding bindActivityFacebook(@Qualifier("activityQueue") Queue activityQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(activityQueue).to(eventsExchange).with("facebook.activity.*");
	}

	@Bean
	public Binding bindCampaignFacebook(@Qualifier("campaignQueue") Queue campaignQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(campaignQueue).to(eventsExchange).with("facebook.campaign.*");
	}

	@Bean
	public Binding bindSecurityDesktop(@Qualifier("securityQueue") Queue securityQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(securityQueue).to(eventsExchange).with("desktop.security.*");
	}

	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
		converter.setClassMapper(rabbitClassMapper());
		return converter;
	}

	@Bean
	public ClassMapper rabbitClassMapper() {
		DefaultClassMapper mapper = new DefaultClassMapper();
		mapper.setTrustedPackages("*");
		Map<String, Class<?>> idMapping = new HashMap<>();
		// Mapeia o __TypeId__ vindo do desktop para o nosso DTO
		idMapping.put("br.com.flowlinkerAPI.dto.event.EnrichedEventDTO", EnrichedEventDTO.class);
		mapper.setIdClassMapping(idMapping);
		return mapper;
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(messageConverter);
		return template;
	}
}


