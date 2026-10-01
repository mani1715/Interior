package com.interior.platform.media.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class S3StorageProperties {

    private String provider = "LOCAL"; // "LOCAL" or "S3_COMPATIBLE"
    private String publicBaseUrl = "/api/v1/media/public";
    private S3Properties s3 = new S3Properties();

    public static class S3Properties {
        private String endpoint;
        private String region = "auto";
        private String accessKeyId;
        private String secretAccessKey;
        private String privateBucket = "interior-platform-media-private";
        private String publicBucket = "interior-platform-media-public";
        private boolean pathStyleAccess = true;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getPrivateBucket() {
            return privateBucket;
        }

        public void setPrivateBucket(String privateBucket) {
            this.privateBucket = privateBucket;
        }

        public String getPublicBucket() {
            return publicBucket;
        }

        public void setPublicBucket(String publicBucket) {
            this.publicBucket = publicBucket;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public S3Properties getS3() {
        return s3;
    }

    public void setS3(S3Properties s3) {
        this.s3 = s3;
    }
}
