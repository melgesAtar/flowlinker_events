package com.flowlinker.events.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
	private static final int MAX_PAYLOAD_LENGTH = 10_000;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		long start = System.currentTimeMillis();

		ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
		ContentCachingResponseWrapper responseWrapper = wrapResponse(response);

		try {
			filterChain.doFilter(requestWrapper, responseWrapper);
		} finally {
			long durationMs = System.currentTimeMillis() - start;
			logExchange(requestWrapper, responseWrapper, durationMs);
			responseWrapper.copyBodyToResponse();
		}
	}

	private void logExchange(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long durationMs) {
		String method = request.getMethod();
		String uri = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
		String path = request.getRequestURI();
		int status = response.getStatus();

		String reqBody = null;
		String resBody = null;

		// Logs mínimos para endpoints de métricas (sempre uma linha em INFO para 2xx/3xx)
		if (path.startsWith("/metrics")) {
			int statusClass = status / 100;
			if (statusClass == 2 || statusClass == 3) {
				if (log.isInfoEnabled()) {
					log.info("METRICS: method={} uri=\"{}\" status={} durationMs={}", method, uri, status, durationMs);
				}
				return;
			}
			// Para 4xx/5xx aplica política geral abaixo (WARN/ERROR)
		}

		// Para reduzir ruído: só loga corpos em DEBUG; 4xx em WARN e 5xx em ERROR, sem corpo
		if (status >= 500) {
			if (log.isErrorEnabled()) {
				log.error("HTTP EXCHANGE: method={} uri=\"{}\" status={} durationMs={}",
						method, uri, status, durationMs);
			}
		} else if (status >= 400) {
			if (log.isWarnEnabled()) {
				log.warn("HTTP EXCHANGE: method={} uri=\"{}\" status={} durationMs={}",
						method, uri, status, durationMs);
			}
		} else if (log.isDebugEnabled()) {
			reqBody = getSafeBody(request.getContentAsByteArray(), getCharsetFromContentType(request.getContentType()));
			resBody = getSafeBody(response.getContentAsByteArray(), getCharsetFromContentType(response.getContentType()));
			log.debug("HTTP EXCHANGE: method={} uri=\"{}\" status={} durationMs={} requestBody={} responseBody={}",
					method, uri, status, durationMs, reqBody, resBody);
		}
	}

	private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
		if (request instanceof ContentCachingRequestWrapper existing) {
			return existing;
		}
		return new ContentCachingRequestWrapper(request);
	}

	private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
		if (response instanceof ContentCachingResponseWrapper existing) {
			return existing;
		}
		return new ContentCachingResponseWrapper(response);
	}

	private String getSafeBody(byte[] content, Charset charset) {
		if (content == null || content.length == 0) {
			return "";
		}
		String body = new String(content, charset);
		if (body.length() > MAX_PAYLOAD_LENGTH) {
			return body.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
		}
		return body;
	}

	private Charset getCharsetFromContentType(String contentType) {
		try {
			if (contentType != null && contentType.contains("charset=")) {
				String charset = contentType.substring(contentType.indexOf("charset=") + 8).trim();
				return Charset.forName(charset);
			}
		} catch (Exception ignored) {
		}
		return StandardCharsets.UTF_8;
	}
}


