package com.flowlinker.events.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Value("${app.security.metricsApiKey:}")
	private String metricsApiKey;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		ApiKeyAuthFilter apiKeyFilter = new ApiKeyAuthFilter(metricsApiKey, List.of(
				"/metrics/**"
		));

		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/actuator/health", "/actuator/info").permitAll()
					.requestMatchers("/metrics/**").authenticated()
					.anyRequest().authenticated()
			)
			.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}


