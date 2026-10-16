package com.interior.platform.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filter that attaches and propagates request correlation IDs, cleans up MDC on thread completion,
 * and performs safe, sanitized request access logging with PII/secret redaction.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_HEADER_UPPER = "X-Request-ID";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private static final Pattern SANITIZED_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");
    private static final Pattern QUERY_SECRET_PATTERN = Pattern.compile("(?i)(password|secret|token|code|key|api[-_]?key|signature|session)=([^&\\s]*)");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader(REQUEST_ID_HEADER_UPPER);
        }

        if (requestId == null || requestId.isBlank() || !SANITIZED_ID_PATTERN.matcher(requestId.trim()).matches()) {
            requestId = UUID.randomUUID().toString();
        } else {
            requestId = requestId.trim();
        }

        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(REQUEST_ID_HEADER_UPPER, requestId);

        MDC.put("requestId", requestId);
        MDC.put("correlationId", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            logAccessSafely(request, response, requestId, durationMs);

            MDC.remove("requestId");
            MDC.remove("correlationId");
            MDC.clear();
        }
    }

    private void logAccessSafely(HttpServletRequest request, HttpServletResponse response, String requestId, long durationMs) {
        try {
            String method = request.getMethod();
            String uri = sanitizeUri(request.getRequestURI(), request.getQueryString());
            int status = response.getStatus();

            // Low-noise health probe handling
            if ("/api/v1/health".equals(request.getRequestURI()) || "/health".equals(request.getRequestURI()) || request.getRequestURI().contains("/actuator/health")) {
                log.debug("HTTP {} {} status={} duration={}ms [requestId={}]", method, uri, status, durationMs, requestId);
            } else if (status >= 500) {
                log.error("HTTP {} {} status={} duration={}ms [requestId={}]", method, uri, status, durationMs, requestId);
            } else if (status >= 400) {
                log.warn("HTTP {} {} status={} duration={}ms [requestId={}]", method, uri, status, durationMs, requestId);
            } else {
                log.info("HTTP {} {} status={} duration={}ms [requestId={}]", method, uri, status, durationMs, requestId);
            }
        } catch (Exception ignored) {
        }
    }

    public static String sanitizeUri(String requestUri, String queryString) {
        if (requestUri == null) {
            requestUri = "/";
        }
        // Neutralize log injection (strip newlines/carriage returns)
        String cleanUri = requestUri.replace("\r", "").replace("\n", "");

        if (queryString == null || queryString.isBlank()) {
            return cleanUri;
        }

        String cleanQuery = queryString.replace("\r", "").replace("\n", "");
        String redactedQuery = QUERY_SECRET_PATTERN.matcher(cleanQuery).replaceAll("$1=[REDACTED]");
        return cleanUri + "?" + redactedQuery;
    }
}
