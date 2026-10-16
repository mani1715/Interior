package com.interior.platform.ai;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.service.SsrfProtectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class AiSsrfAndOutputSecurityTest {

    private SsrfProtectionService ssrfService;

    @BeforeEach
    void setUp() {
        ssrfService = new SsrfProtectionService();
    }

    @Test
    @DisplayName("Should block loopback URLs (127.0.0.1 and localhost)")
    void validateTargetUri_rejectsLoopbackAddress() {
        assertThrows(BadRequestException.class, () ->
                ssrfService.validateTargetUri(URI.create("http://127.0.0.1/output.jpg")));
        assertThrows(BadRequestException.class, () ->
                ssrfService.validateTargetUri(URI.create("http://localhost:8080/image.png")));
        assertFalse(ssrfService.isSafeRemoteUrl("http://127.0.0.1/internal.png"));
    }

    @Test
    @DisplayName("Should block cloud metadata service URLs (169.254.169.254 and metadata.google.internal)")
    void validateTargetUri_rejectsCloudMetadata() {
        assertThrows(BadRequestException.class, () ->
                ssrfService.validateTargetUri(URI.create("http://169.254.169.254/latest/meta-data/")));
        assertThrows(BadRequestException.class, () ->
                ssrfService.validateTargetUri(URI.create("http://metadata.google.internal/computeMetadata/v1/")));
        assertFalse(ssrfService.isSafeRemoteUrl("http://169.254.169.254/secret"));
    }

    @Test
    @DisplayName("Should block private RFC 1918 IP addresses (10.x, 192.168.x)")
    void validateTargetUri_rejectsPrivateRfc1918() {
        assertThrows(BadRequestException.class, () ->
                ssrfService.validateTargetUri(URI.create("http://10.0.0.5/image.jpg")));
        assertThrows(BadRequestException.class, () ->
                ssrfService.validateTargetUri(URI.create("http://192.168.1.1/router.png")));
        assertFalse(ssrfService.isSafeRemoteUrl("http://10.0.0.1/payload"));
    }

    @Test
    @DisplayName("Should block non-HTTP/HTTPS protocols (file, ftp, gopher)")
    void validateTargetUri_rejectsNonHttpSchemes() {
        assertThrows(BadRequestException.class, () ->
                ssrfService.validateTargetUri(URI.create("file:///etc/passwd")));
        assertThrows(BadRequestException.class, () ->
                ssrfService.validateTargetUri(URI.create("ftp://ftp.example.com/file.jpg")));
        assertFalse(ssrfService.isSafeRemoteUrl("file:///c:/windows/system32/config/sam"));
    }

    @Test
    @DisplayName("Should reject empty or null remote image URL")
    void downloadBoundedImage_rejectsEmptyUrl() {
        assertThrows(BadRequestException.class, () ->
                ssrfService.downloadBoundedImage("", 1024L));
        assertThrows(BadRequestException.class, () ->
                ssrfService.downloadBoundedImage(null, 1024L));
    }
}
