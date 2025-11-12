package com.flowlinker.events.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.flowlinker.events.api.dto.EnrichedEventDTO;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitConfig {

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
}


