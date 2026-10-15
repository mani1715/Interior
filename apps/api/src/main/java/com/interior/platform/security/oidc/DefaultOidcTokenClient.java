package com.interior.platform.security.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.config.AuthSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Standard HTTP implementation of OIDC token exchange client using Java 21 HTTP Client.
 * Adheres strictly to RFC 6749 (OAuth 2.0) and RFC 7636 (PKCE S256).
 * Secrets and tokens are never logged or exposed in audit trails.
 */
@Component
public class DefaultOidcTokenClient implements OidcTokenClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultOidcTokenClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DefaultOidcTokenClient() {
        this(new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultOidcTokenClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    public DefaultOidcTokenClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = httpClient;
    }

    @Override
    public OidcTokenResponse exchangeAuthorizationCode(
            AuthSecurityProperties.OidcProviderProperties provider,
            String code,
            String codeVerifier
    ) {
        if (provider == null || provider.getTokenUri() == null || provider.getTokenUri().isBlank()) {
            throw new IllegalStateException("OIDC token endpoint URI is not configured");
        }
        if (code == null || code.isBlank()) {
            throw new AccessDeniedException("Authorization code is missing");
        }
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new AccessDeniedException("PKCE code verifier is missing");
        }

        StringBuilder form = new StringBuilder();
        form.append("grant_type=authorization_code");
        form.append("&code=").append(urlEncode(code));
        form.append("&redirect_uri=").append(urlEncode(provider.getRedirectUri()));
        form.append("&client_id=").append(urlEncode(provider.getClientId()));
        form.append("&client_secret=").append(urlEncode(provider.getClientSecret()));
        form.append("&code_verifier=").append(urlEncode(codeVerifier));

        int timeoutMs = provider.getReadTimeoutMs() > 0 ? provider.getReadTimeoutMs() : 5000;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(provider.getTokenUri()))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString(), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();

            if (status != 200) {
                log.warn("OIDC token endpoint responded with HTTP status {}", status);
                throw new AccessDeniedException("OIDC token exchange failed with provider (HTTP " + status + ")");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode idTokenNode = root.get("id_token");
            if (idTokenNode == null || idTokenNode.asText().isBlank()) {
                throw new AccessDeniedException("ID token missing from provider token response");
            }

            String idToken = idTokenNode.asText();
            String accessToken = root.has("access_token") ? root.get("access_token").asText() : null;
            String tokenType = root.has("token_type") ? root.get("token_type").asText() : "Bearer";
            Long expiresIn = root.has("expires_in") ? root.get("expires_in").asLong() : null;

            return new OidcTokenResponse(accessToken, idToken, tokenType, expiresIn);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to connect to OIDC token endpoint at {}: {}", provider.getTokenUri(), e.getMessage());
            throw new IllegalStateException("OIDC token endpoint communication failure", e);
        }
    }

    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
