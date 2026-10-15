package com.interior.platform.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.media.security")
public class MediaSecurityProperties {

    /**
     * Scanner provider implementation: "disabled", "fake", "clamav"
     */
    private String scanner = "fake";

    /**
     * Whether a verified CLEAN scan is required before committing uploads to canonical storage.
     * Default true for secure production and fail-closed posture.
     */
    private boolean requireCleanScan = true;

    /**
     * Maximum execution time for a scan in milliseconds before timing out.
     */
    private long scanTimeoutMs = 10000;

    /**
     * Maximum retry attempts for transient scan failures.
     */
    private int maxScanRetries = 2;

    private ClamAvProperties clamav = new ClamAvProperties();

    public static class ClamAvProperties {
        private String host = "localhost";
        private int port = 3310;
        private int connectionTimeoutMs = 3000;
        private int readTimeoutMs = 10000;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        public void setConnectionTimeoutMs(int connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public String getScanner() {
        return scanner;
    }

    public void setScanner(String scanner) {
        this.scanner = scanner;
    }

    public boolean isRequireCleanScan() {
        return requireCleanScan;
    }

    public void setRequireCleanScan(boolean requireCleanScan) {
        this.requireCleanScan = requireCleanScan;
    }

    public long getScanTimeoutMs() {
        return scanTimeoutMs;
    }

    public void setScanTimeoutMs(long scanTimeoutMs) {
        this.scanTimeoutMs = scanTimeoutMs;
    }

    public int getMaxScanRetries() {
        return maxScanRetries;
    }

    public void setMaxScanRetries(int maxScanRetries) {
        this.maxScanRetries = maxScanRetries;
    }

    public ClamAvProperties getClamav() {
        return clamav;
    }

    public void setClamav(ClamAvProperties clamav) {
        this.clamav = clamav;
    }
}
