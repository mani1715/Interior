package com.interior.platform.security.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * High-performance, in-process Mock OpenID Connect Identity Provider.
 * Serves real RFC 7517 JWKS, discovery, and RFC 6749 / 7636 token endpoints with RS256 cryptographic signatures.
 */
public class MockOidcServer implements AutoCloseable {

    private final HttpServer server;
    private final int port;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KeyPair keyPair1;
    private final String kid1 = "test-key-1";

    private KeyPair keyPair2;
    private final String kid2 = "test-key-2";
    private final AtomicBoolean keyRotationActive = new AtomicBoolean(false);

    private final AtomicReference<String> overrideTokenResponse = new AtomicReference<>(null);
    private final AtomicInteger overrideTokenStatus = new AtomicInteger(200);
    private final AtomicInteger tokenDelayMs = new AtomicInteger(0);

    public MockOidcServer() throws IOException, NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        this.keyPair1 = generator.generateKeyPair();
        this.keyPair2 = generator.generateKeyPair();

        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.port = server.getAddress().getPort();
        this.baseUrl = "http://127.0.0.1:" + port;

        setupEndpoints();
        this.server.start();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getIssuer() {
        return baseUrl;
    }

    public String getAuthorizationUri() {
        return baseUrl + "/authorize";
    }

    public String getTokenUri() {
        return baseUrl + "/token";
    }

    public String getJwkSetUri() {
        return baseUrl + "/jwks";
    }

    public void enableKeyRotation() {
        keyRotationActive.set(true);
    }

    public void setTokenResponseOverride(int statusCode, String body) {
        this.overrideTokenStatus.set(statusCode);
        this.overrideTokenResponse.set(body);
    }

    public void setTokenDelayMs(int delayMs) {
        this.tokenDelayMs.set(delayMs);
    }

    public KeyPair getKeyPair1() {
        return keyPair1;
    }

    public KeyPair getKeyPair2() {
        return keyPair2;
    }

    public String getKid1() {
        return kid1;
    }

    public String getKid2() {
        return kid2;
    }

    public String createSignedIdToken(
            String issuer,
            String audience,
            String subject,
            String nonce,
            String email,
            String name,
            boolean emailVerified,
            Instant expiresAt,
            Instant issuedAt,
            String kid,
            KeyPair keyPair
    ) {
        try {
            Map<String, Object> header = Map.of(
                    "alg", "RS256",
                    "typ", "JWT",
                    "kid", kid
            );

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("iss", issuer);
            payload.put("aud", audience);
            payload.put("sub", subject);
            if (nonce != null) payload.put("nonce", nonce);
            payload.put("exp", expiresAt.getEpochSecond());
            payload.put("iat", issuedAt.getEpochSecond());
            if (email != null) payload.put("email", email);
            payload.put("email_verified", emailVerified);
            if (name != null) payload.put("name", name);

            String headerJson = objectMapper.writeValueAsString(header);
            String payloadJson = objectMapper.writeValueAsString(payload);

            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = encodedHeader + "." + encodedPayload;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            byte[] signatureBytes = signature.sign();

            String encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
            return signingInput + "." + encodedSignature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test signed ID token", e);
        }
    }

    private void setupEndpoints() {
        // 1. Discovery
        server.createContext("/.well-known/openid-configuration", exchange -> {
            Map<String, Object> config = Map.of(
                    "issuer", baseUrl,
                    "authorization_endpoint", baseUrl + "/authorize",
                    "token_endpoint", baseUrl + "/token",
                    "jwks_uri", baseUrl + "/jwks",
                    "response_types_supported", List.of("code"),
                    "id_token_signing_alg_values_supported", List.of("RS256")
            );
            sendJsonResponse(exchange, 200, config);
        });

        // 2. JWKS
        server.createContext("/jwks", exchange -> {
            List<Map<String, Object>> keys = new ArrayList<>();
            keys.add(createJwk((RSAPublicKey) keyPair1.getPublic(), kid1));
            if (keyRotationActive.get()) {
                keys.add(createJwk((RSAPublicKey) keyPair2.getPublic(), kid2));
            }
            sendJsonResponse(exchange, 200, Map.of("keys", keys));
        });

        // 3. Token Endpoint
        server.createContext("/token", exchange -> {
            if (tokenDelayMs.get() > 0) {
                try {
                    Thread.sleep(tokenDelayMs.get());
                } catch (InterruptedException ignored) {}
            }

            if (overrideTokenResponse.get() != null) {
                sendRawResponse(exchange, overrideTokenStatus.get(), overrideTokenResponse.get());
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendRawResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = parseFormBody(body);

            String code = form.get("code");
            String clientId = form.get("client_id");
            String codeVerifier = form.get("code_verifier");

            // Generate standard valid ID token
            Instant now = Instant.now();
            String idToken = createSignedIdToken(
                    baseUrl,
                    clientId != null ? clientId : "test-client",
                    "mock-sub-12345",
                    "test-nonce-123",
                    "user@example.com",
                    "Test User",
                    true,
                    now.plusSeconds(3600),
                    now,
                    kid1,
                    keyPair1
            );

            Map<String, Object> response = Map.of(
                    "access_token", "mock-access-token-" + UUID.randomUUID(),
                    "token_type", "Bearer",
                    "expires_in", 3600,
                    "id_token", idToken
            );

            sendJsonResponse(exchange, 200, response);
        });
    }

    private Map<String, Object> createJwk(RSAPublicKey publicKey, String kid) {
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(toUnsignedByteArray(publicKey.getModulus()));
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(toUnsignedByteArray(publicKey.getPublicExponent()));

        return Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", kid,
                "n", n,
                "e", e
        );
    }

    private byte[] toUnsignedByteArray(BigInteger bi) {
        byte[] bytes = bi.toByteArray();
        if (bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return bytes;
    }

    private Map<String, String> parseFormBody(String body) {
        Map<String, String> result = new HashMap<>();
        if (body == null || body.isBlank()) return result;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            } else if (kv.length == 1) {
                result.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), "");
            }
        }
        return result;
    }

    private void sendJsonResponse(HttpExchange exchange, int status, Object data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendRawResponse(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
