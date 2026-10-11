package com.interior.platform.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.security")
public class AuthSecurityProperties {

    private String sessionCookieName = "__Host-session";
    private boolean sessionCookieSecure = true;
    private long sessionIdleTimeoutSeconds = 1800; // 30 mins
    private long sessionAbsoluteTimeoutSeconds = 43200; // 12 hours
    private long sessionLastSeenUpdateIntervalSeconds = 300; // 5 mins throttled
    private boolean devAuthEnabled = false;

    private OidcProperties oidc = new OidcProperties();

    public static class OidcProperties {
        private Map<String, OidcProviderProperties> providers = new HashMap<>();

        public Map<String, OidcProviderProperties> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, OidcProviderProperties> providers) {
            this.providers = providers;
        }
    }

    public static class OidcProviderProperties {
        private String issuer;
        private String clientId;
        private String clientSecret;
        private String authorizationUri;
        private String tokenUri;
        private String redirectUri;
        private List<String> scopes = List.of("openid", "email", "profile");

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }
    }

    // Getters and Setters
    public String getSessionCookieName() {
        return sessionCookieName;
    }

    public void setSessionCookieName(String sessionCookieName) {
        this.sessionCookieName = sessionCookieName;
    }

    public boolean isSessionCookieSecure() {
        return sessionCookieSecure;
    }

    public void setSessionCookieSecure(boolean sessionCookieSecure) {
        this.sessionCookieSecure = sessionCookieSecure;
    }

    public long getSessionIdleTimeoutSeconds() {
        return sessionIdleTimeoutSeconds;
    }

    public void setSessionIdleTimeoutSeconds(long sessionIdleTimeoutSeconds) {
        this.sessionIdleTimeoutSeconds = sessionIdleTimeoutSeconds;
    }

    public long getSessionAbsoluteTimeoutSeconds() {
        return sessionAbsoluteTimeoutSeconds;
    }

    public void setSessionAbsoluteTimeoutSeconds(long sessionAbsoluteTimeoutSeconds) {
        this.sessionAbsoluteTimeoutSeconds = sessionAbsoluteTimeoutSeconds;
    }

    public long getSessionLastSeenUpdateIntervalSeconds() {
        return sessionLastSeenUpdateIntervalSeconds;
    }

    public void setSessionLastSeenUpdateIntervalSeconds(long sessionLastSeenUpdateIntervalSeconds) {
        this.sessionLastSeenUpdateIntervalSeconds = sessionLastSeenUpdateIntervalSeconds;
    }

    public boolean isDevAuthEnabled() {
        return devAuthEnabled;
    }

    public void setDevAuthEnabled(boolean devAuthEnabled) {
        this.devAuthEnabled = devAuthEnabled;
    }

    private MfaProperties mfa = new MfaProperties();

    public static class MfaProperties {
        private boolean enabled = true;
        private String encryptionKey = "elegance-default-dev-mfa-key-32b!"; // 32 bytes for dev/test
        private int currentKeyVersion = 1;
        private String issuer = "Elégance";
        private int pendingEnrollmentTtlSeconds = 600; // 10 minutes
        private int stepUpFreshnessSeconds = 900; // 15 minutes
        private int allowedClockSkewSteps = 1; // ±1 step = ±30s
        private int maxFailedAttempts = 5;
        private int lockoutDurationSeconds = 900; // 15 minutes
        private int recoveryCodesCount = 10;
        private boolean moderatorMfaRequired = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEncryptionKey() {
            return encryptionKey;
        }

        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        public int getCurrentKeyVersion() {
            return currentKeyVersion;
        }

        public void setCurrentKeyVersion(int currentKeyVersion) {
            this.currentKeyVersion = currentKeyVersion;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public int getPendingEnrollmentTtlSeconds() {
            return pendingEnrollmentTtlSeconds;
        }

        public void setPendingEnrollmentTtlSeconds(int pendingEnrollmentTtlSeconds) {
            this.pendingEnrollmentTtlSeconds = pendingEnrollmentTtlSeconds;
        }

        public int getStepUpFreshnessSeconds() {
            return stepUpFreshnessSeconds;
        }

        public void setStepUpFreshnessSeconds(int stepUpFreshnessSeconds) {
            this.stepUpFreshnessSeconds = stepUpFreshnessSeconds;
        }

        public int getAllowedClockSkewSteps() {
            return allowedClockSkewSteps;
        }

        public void setAllowedClockSkewSteps(int allowedClockSkewSteps) {
            this.allowedClockSkewSteps = allowedClockSkewSteps;
        }

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public int getLockoutDurationSeconds() {
            return lockoutDurationSeconds;
        }

        public void setLockoutDurationSeconds(int lockoutDurationSeconds) {
            this.lockoutDurationSeconds = lockoutDurationSeconds;
        }

        public int getRecoveryCodesCount() {
            return recoveryCodesCount;
        }

        public void setRecoveryCodesCount(int recoveryCodesCount) {
            this.recoveryCodesCount = recoveryCodesCount;
        }

        public boolean isModeratorMfaRequired() {
            return moderatorMfaRequired;
        }

        public void setModeratorMfaRequired(boolean moderatorMfaRequired) {
            this.moderatorMfaRequired = moderatorMfaRequired;
        }
    }

    public MfaProperties getMfa() {
        return mfa;
    }

    public void setMfa(MfaProperties mfa) {
        this.mfa = mfa;
    }

    public OidcProperties getOidc() {
        return oidc;
    }

    public void setOidc(OidcProperties oidc) {
        this.oidc = oidc;
    }
}
