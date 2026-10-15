package com.interior.platform.security.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe caching JWKS resolver supporting key rotation and standard RFC 7517 RSA keys.
 */
@Component
public class DefaultJwksResolver implements JwksResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultJwksResolver.class);
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(1);

    private record JwksCacheEntry(Map<String, PublicKey> keys, Instant expiresAt) {}

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final HttpClient httpClient;
    private final Map<String, JwksCacheEntry> cache = new ConcurrentHashMap<>();

    public DefaultJwksResolver() {
        this(new ObjectMapper(), Clock.systemUTC());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultJwksResolver(ObjectMapper objectMapper, Clock clock) {
        this(objectMapper, clock, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public DefaultJwksResolver(ObjectMapper objectMapper, Clock clock, HttpClient httpClient) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.httpClient = httpClient;
    }

    @Override
    public PublicKey resolvePublicKey(String jwkSetUri, String kid) {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            throw new IllegalStateException("OIDC JWKS URI is not configured");
        }
        if (kid == null || kid.isBlank()) {
            throw new AccessDeniedException("Key ID (kid) is missing from OIDC token header");
        }

        Instant now = clock.instant();
        JwksCacheEntry cached = cache.get(jwkSetUri);

        // Check cache hit
        if (cached != null && cached.expiresAt().isAfter(now) && cached.keys().containsKey(kid)) {
            return cached.keys().get(kid);
        }

        // Cache miss or key rotation: fetch fresh JWKS
        Map<String, PublicKey> freshKeys = fetchJwks(jwkSetUri);
        cache.put(jwkSetUri, new JwksCacheEntry(freshKeys, now.plus(DEFAULT_CACHE_TTL)));

        PublicKey key = freshKeys.get(kid);
        if (key == null) {
            log.warn("Key ID '{}' not found in JWKS from {}", kid, jwkSetUri);
            throw new AccessDeniedException("OIDC token signed by unknown key ID: " + kid);
        }

        return key;
    }

    @Override
    public void invalidateCache(String jwkSetUri) {
        if (jwkSetUri != null) {
            cache.remove(jwkSetUri);
        }
    }

    private Map<String, PublicKey> fetchJwks(String jwkSetUri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwkSetUri))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Failed to retrieve JWKS from " + jwkSetUri + " (HTTP " + response.statusCode() + ")");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode keysNode = root.get("keys");
            if (keysNode == null || !keysNode.isArray()) {
                throw new IllegalStateException("Invalid JWKS response format: missing 'keys' array");
            }

            Map<String, PublicKey> result = new HashMap<>();
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            for (JsonNode keyNode : keysNode) {
                String kty = keyNode.has("kty") ? keyNode.get("kty").asText() : "";
                if (!"RSA".equalsIgnoreCase(kty)) {
                    continue; // Only RSA keys supported for standard OIDC RS256
                }

                String kid = keyNode.has("kid") ? keyNode.get("kid").asText() : null;
                String n = keyNode.has("n") ? keyNode.get("n").asText() : null;
                String e = keyNode.has("e") ? keyNode.get("e").asText() : null;

                if (kid != null && n != null && e != null) {
                    try {
                        byte[] nBytes = Base64.getUrlDecoder().decode(n);
                        byte[] eBytes = Base64.getUrlDecoder().decode(e);
                        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
                        PublicKey publicKey = keyFactory.generatePublic(spec);
                        result.put(kid, publicKey);
                    } catch (IllegalArgumentException | InvalidKeySpecException ex) {
                        log.warn("Skipping malformed JWK key '{}': {}", kid, ex.getMessage());
                    }
                }
            }

            return result;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to communicate with JWKS endpoint: " + jwkSetUri, ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm not supported on this runtime", ex);
        }
    }
}
