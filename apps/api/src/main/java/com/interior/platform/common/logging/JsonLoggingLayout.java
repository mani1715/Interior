package com.interior.platform.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance, zero-dependency structured JSON layout for production logging.
 * Guarantees machine-parseable JSON per line, neutralizes log injection (CR/LF attacks),
 * redacts secrets/credentials, masks PII, and bounds message sizes.
 */
public class JsonLoggingLayout extends LayoutBase<ILoggingEvent> {

    private static final String SERVICE_NAME = "interior-platform-api";
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private static final int MAX_STACK_TRACE_FRAMES = 25;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");
    private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENSITIVE_KEY_VALUE_PATTERN = Pattern.compile("(?i)(password|secret|token|api[-_]?key|auth[-_]?code|code|session|session[-_]?id|access[-_]?token|id[-_]?token|signature)=([^\\s&,;\"]+)");

    private String environment = "production";

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getEnvironment() {
        return environment;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{");

        // 1. Timestamp (ISO-8601 UTC)
        String timestamp = Instant.ofEpochMilli(event.getTimeStamp()).toString();
        appendField(sb, "timestamp", timestamp);

        // 2. Level
        appendField(sb, "level", event.getLevel() != null ? event.getLevel().toString() : "INFO");

        // 3. Service & Environment
        appendField(sb, "service", SERVICE_NAME);
        appendField(sb, "environment", environment);

        // 4. Thread & Logger
        appendField(sb, "thread", event.getThreadName());
        appendField(sb, "logger", event.getLoggerName());

        // 5. Correlation & Request ID from MDC
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            String requestId = mdc.get("requestId");
            if (requestId == null || requestId.isBlank()) {
                requestId = mdc.get("correlationId");
            }
            if (requestId != null && !requestId.isBlank()) {
                appendField(sb, "requestId", requestId);
            }
        }

        // 6. Sanitized & Redacted Message
        String rawMessage = event.getFormattedMessage();
        String sanitizedMessage = sanitizeAndRedact(rawMessage);
        appendField(sb, "message", sanitizedMessage);

        // 7. Exception details if present
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            String exDetails = formatThrowable(tp);
            appendField(sb, "exception", exDetails);
        }

        // Remove trailing comma
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.append("}\n");
        return sb.toString();
    }

    public static String sanitizeAndRedact(String input) {
        if (input == null) {
            return "";
        }

        // Neutralize log injection: replace carriage returns and newlines with space
        String sanitized = input.replace("\r", " ").replace("\n", " ");

        // Bounded length protection
        if (sanitized.length() > MAX_MESSAGE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_MESSAGE_LENGTH) + " ... [TRUNCATED]";
        }

        // Redact Bearer tokens
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED]");

        // Redact key-value sensitive patterns (password=..., secret=..., token=..., code=..., key=...)
        sanitized = SENSITIVE_KEY_VALUE_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED]");

        // Mask email addresses: j***@example.com
        Matcher emailMatcher = EMAIL_PATTERN.matcher(sanitized);
        StringBuffer emailMasked = new StringBuffer();
        while (emailMatcher.find()) {
            String firstChar = emailMatcher.group(1);
            String domain = emailMatcher.group(2);
            emailMatcher.appendReplacement(emailMasked, firstChar + "***@" + domain);
        }
        emailMatcher.appendTail(emailMasked);

        return emailMasked.toString();
    }

    private String formatThrowable(IThrowableProxy tp) {
        StringBuilder sb = new StringBuilder();
        sb.append(tp.getClassName()).append(": ").append(sanitizeAndRedact(tp.getMessage()));

        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
        if (stepArray != null) {
            int frames = Math.min(stepArray.length, MAX_STACK_TRACE_FRAMES);
            for (int i = 0; i < frames; i++) {
                sb.append(" | at ").append(stepArray[i].getSTEAsString());
            }
            if (stepArray.length > frames) {
                sb.append(" | ... and ").append(stepArray.length - frames).append(" more frames");
            }
        }
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String key, String value) {
        if (value == null) {
            return;
        }
        sb.append("\"").append(escapeJson(key)).append("\":\"").append(escapeJson(value)).append("\",");
    }

    public static String escapeJson(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
