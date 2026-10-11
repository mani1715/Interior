package com.interior.platform.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_HEADER_UPPER = "X-Request-ID";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    private static final java.util.regex.Pattern SANITIZED_ID_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

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

        org.slf4j.MDC.put("requestId", requestId);
        org.slf4j.MDC.put("correlationId", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove("requestId");
            org.slf4j.MDC.remove("correlationId");
        }
    }
}
