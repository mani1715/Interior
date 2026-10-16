package com.interior.platform.email.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Validates Amazon SNS cryptographic signatures and protects against SSRF attacks.
 * Features:
 *  - Strict HTTPS certificate host whitelist: sns.<region>.amazonaws.com
 *  - Rejection of non-HTTPS or non-Amazon signing URLs
 *  - Canonical string builder per AWS SNS specification
 *  - SHA1withRSA (v1) and SHA256withRSA (v2) signature verification
 *  - Optional TopicArn whitelist enforcement
 *  - Safe SubscribeURL verification
 *  - In-memory certificate caching with size bounds
 *  - MessageId deduplication to prevent replay attacks
 */
@Component
public class AwsSnsSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(AwsSnsSignatureValidator.class);

    // Whitelist pattern for valid AWS SNS signing certificate URLs (SSRF prevention)
    private static final Pattern AWS_SNS_HOST_PATTERN = Pattern.compile(
            "^sns\\.[a-zA-Z0-9-]+\\.amazonaws\\.com(\\.cn)?$"
    );

    private static final Pattern AWS_TOPIC_ARN_PATTERN = Pattern.compile(
            "^arn:aws[a-z-]*:sns:[a-z0-9-]+:[0-9]{12}:[a-zA-Z0-9-_.]+$"
    );

    private final HttpClient httpClient;
    private final Map<String, X509Certificate> certificateCache = new ConcurrentHashMap<>();
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();
    private static final int MAX_CACHE_SIZE = 100;
    private static final int MAX_MESSAGE_ID_CACHE = 5000;

    public AwsSnsSignatureValidator() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER) // Disallow redirects to prevent SSRF hopping
                .build();
    }

    // Constructor for testing with mock HttpClient
    public AwsSnsSignatureValidator(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Checks if the SNS message ID was already processed (replay defense).
     */
    public boolean isDuplicateMessage(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return false;
        }
        if (processedMessageIds.contains(messageId)) {
            return true;
        }
        if (processedMessageIds.size() > MAX_MESSAGE_ID_CACHE) {
            processedMessageIds.clear(); // Reset when bounded threshold reached
        }
        processedMessageIds.add(messageId);
        return false;
    }

    /**
     * Authenticates an incoming AWS SNS message.
     */
    public boolean isValid(JsonNode snsPayload, String expectedTopicArn) {
        if (snsPayload == null || !snsPayload.isObject()) {
            log.warn("SNS validation rejected: payload is not a valid JSON object");
            return false;
        }

        String type = getTextField(snsPayload, "Type");
        String messageId = getTextField(snsPayload, "MessageId");
        String topicArn = getTextField(snsPayload, "TopicArn");
        String message = getTextField(snsPayload, "Message");
        String timestamp = getTextField(snsPayload, "Timestamp");
        String signatureVersion = getTextField(snsPayload, "SignatureVersion");
        String signature = getTextField(snsPayload, "Signature");
        String signingCertUrl = getTextField(snsPayload, "SigningCertURL");

        if (type == null || messageId == null || topicArn == null || message == null ||
                timestamp == null || signatureVersion == null || signature == null || signingCertUrl == null) {
            log.warn("SNS validation rejected: missing mandatory SNS signature fields");
            return false;
        }

        // 1. Validate TopicArn
        if (!AWS_TOPIC_ARN_PATTERN.matcher(topicArn).matches()) {
            log.warn("SNS validation rejected: TopicArn does not match valid AWS SNS ARN format: {}", topicArn);
            return false;
        }
        if (expectedTopicArn != null && !expectedTopicArn.isBlank() && !expectedTopicArn.equals(topicArn)) {
            log.warn("SNS validation rejected: TopicArn '{}' does not match expected '{}'", topicArn, expectedTopicArn);
            return false;
        }

        // 2. Validate SigningCertURL against SSRF Whitelist
        if (!isSafeSigningCertUrl(signingCertUrl)) {
            log.warn("SNS validation rejected: SigningCertURL failed SSRF domain whitelist: {}", signingCertUrl);
            return false;
        }

        // 3. Build Canonical Message
        String canonicalString;
        try {
            canonicalString = buildCanonicalString(snsPayload, type);
        } catch (IllegalArgumentException e) {
            log.warn("SNS validation rejected: failed to construct canonical string: {}", e.getMessage());
            return false;
        }

        // 4. Retrieve and Parse Signing Certificate
        X509Certificate cert = getCertificate(signingCertUrl);
        if (cert == null) {
            log.error("SNS validation rejected: unable to retrieve or parse signing certificate from {}", signingCertUrl);
            return false;
        }

        // 5. Verify Cryptographic Signature
        try {
            String algorithm;
            if ("1".equals(signatureVersion)) {
                algorithm = "SHA1withRSA";
            } else if ("2".equals(signatureVersion)) {
                algorithm = "SHA256withRSA";
            } else {
                log.warn("SNS validation rejected: unsupported SignatureVersion: {}", signatureVersion);
                return false;
            }

            Signature sig = Signature.getInstance(algorithm);
            sig.initVerify(cert.getPublicKey());
            sig.update(canonicalString.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            boolean verified = sig.verify(signatureBytes);

            if (!verified) {
                log.warn("SNS validation rejected: cryptographic signature verification failed for MessageId={}", messageId);
            }
            return verified;
        } catch (Exception e) {
            log.error("SNS validation error verifying signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * SSRF defense: enforces HTTPS and official AWS SNS certificate domains.
     */
    public boolean isSafeSigningCertUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || !AWS_SNS_HOST_PATTERN.matcher(host).matches()) {
                return false;
            }
            String path = uri.getPath();
            if (path == null || !path.endsWith(".pem")) {
                return false;
            }
            // Disallow userinfo, query, port
            return uri.getUserInfo() == null && uri.getQuery() == null && uri.getPort() == -1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * SSRF defense for SNS Subscription Confirmation URLs.
     */
    public boolean isSafeSubscribeUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost();
            return host != null && AWS_SNS_HOST_PATTERN.matcher(host).matches();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Builds canonical string for signature verification according to AWS SNS specification.
     */
    private String buildCanonicalString(JsonNode root, String type) {
        StringBuilder sb = new StringBuilder();
        if ("Notification".equals(type)) {
            appendField(sb, "Message", root);
            appendField(sb, "MessageId", root);
            if (root.has("Subject") && !root.get("Subject").isNull()) {
                appendField(sb, "Subject", root);
            }
            appendField(sb, "Timestamp", root);
            appendField(sb, "TopicArn", root);
            appendField(sb, "Type", root);
        } else if ("SubscriptionConfirmation".equals(type) || "UnsubscribeConfirmation".equals(type)) {
            appendField(sb, "Message", root);
            appendField(sb, "MessageId", root);
            appendField(sb, "SubscribeURL", root);
            appendField(sb, "Timestamp", root);
            appendField(sb, "Token", root);
            appendField(sb, "TopicArn", root);
            appendField(sb, "Type", root);
        } else {
            throw new IllegalArgumentException("Unknown SNS message type: " + type);
        }
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String fieldName, JsonNode root) {
        if (!root.has(fieldName) || root.get(fieldName).isNull()) {
            throw new IllegalArgumentException("Missing required canonical field: " + fieldName);
        }
        sb.append(fieldName).append("\n").append(root.get(fieldName).asText()).append("\n");
    }

    private X509Certificate getCertificate(String certUrl) {
        if (certificateCache.containsKey(certUrl)) {
            return certificateCache.get(certUrl);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(certUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.error("Failed to fetch SNS signing cert: HTTP {}", response.statusCode());
                return null;
            }

            byte[] certBytes = response.body();
            if (certBytes.length > 100_000) { // 100KB limit
                log.error("SNS certificate size exceeds limit: {} bytes", certBytes.length);
                return null;
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (InputStream in = new ByteArrayInputStream(certBytes)) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                if (certificateCache.size() > MAX_CACHE_SIZE) {
                    certificateCache.clear();
                }
                certificateCache.put(certUrl, cert);
                return cert;
            }
        } catch (Exception e) {
            log.error("Exception fetching SNS certificate from {}: {}", certUrl, e.getMessage());
            return null;
        }
    }

    // Helper for injecting test certificates into the cache during unit testing
    public void cacheCertificateForTesting(String certUrl, X509Certificate cert) {
        this.certificateCache.put(certUrl, cert);
    }

    private String getTextField(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return null;
    }
}
