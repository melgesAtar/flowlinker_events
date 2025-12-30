package com.flowlinker.events.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Filtro simples de API Key via header X-API-KEY para proteger endpoints específicos.
 * Define um Authentication com ROLE_METRICS quando a chave é válida.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	private final String requiredApiKey;
	private final List<String> protectedPatterns;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public ApiKeyAuthFilter(String requiredApiKey, List<String> protectedPatterns) {
		this.requiredApiKey = requiredApiKey;
		this.protectedPatterns = protectedPatterns;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		// Filtra apenas para os padrões protegidos
		return protectedPatterns.stream().noneMatch(p -> pathMatcher.match(p, path));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Se não há chave configurada, negar por segurança
		if (requiredApiKey == null || requiredApiKey.isBlank()) {
			deny(response, "API key não configurada");
			return;
		}

		String apiKey = request.getHeader("X-API-KEY");

		if (!Objects.equals(requiredApiKey, apiKey)) {
			deny(response, "API key inválida ou ausente");
			return;
		}

		// Autentica como serviço de métricas
		Authentication auth = new UsernamePasswordAuthenticationToken(
				"metrics-client",
				"",
				List.of(new SimpleGrantedAuthority("ROLE_METRICS"))
		);
		SecurityContextHolder.getContext().setAuthentication(auth);

		// Opcional: adiciona cabeçalho padrão para indicar modo de autenticação
		response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey realm=\"metrics\"");

		filterChain.doFilter(request, response);
	}

	private void deny(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}");
	}
}


