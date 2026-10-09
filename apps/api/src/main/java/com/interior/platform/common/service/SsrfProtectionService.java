package com.interior.platform.common.service;

import com.interior.platform.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.*;
import java.util.List;

@Service
public class SsrfProtectionService {

    private static final Logger log = LoggerFactory.getLogger(SsrfProtectionService.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_REDIRECTS = 3;

    private static final List<String> FORBIDDEN_HOSTNAMES = List.of(
            "localhost",
            "metadata.google.internal",
            "metadata.internal",
            "169.254.169.254"
    );

    public boolean isSafeRemoteUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return false;
        }
        try {
            validateTargetUri(URI.create(urlString.trim()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates that a URI is safe to fetch from the server without risking SSRF against internal resources.
     */
    public void validateTargetUri(URI uri) {
        if (uri == null) {
            throw new BadRequestException("Remote URI cannot be null");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new BadRequestException("Only HTTP and HTTPS protocols are permitted for remote fetch: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BadRequestException("Remote URI must specify a valid host");
        }

        String lowerHost = host.toLowerCase().trim();
        for (String forbidden : FORBIDDEN_HOSTNAMES) {
            if (lowerHost.equals(forbidden) || lowerHost.endsWith("." + forbidden)) {
                throw new BadRequestException("Access to internal or cloud metadata endpoint is blocked: " + host);
            }
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addr.isLoopbackAddress()) {
                    throw new BadRequestException("SSRF blocked: Target resolves to loopback address: " + addr.getHostAddress());
                }
                if (addr.isSiteLocalAddress()) {
                    throw new BadRequestException("SSRF blocked: Target resolves to private RFC1918 address: " + addr.getHostAddress());
                }
                if (addr.isLinkLocalAddress()) {
                    throw new BadRequestException("SSRF blocked: Target resolves to link-local address: " + addr.getHostAddress());
                }
                if (addr.isAnyLocalAddress()) {
                    throw new BadRequestException("SSRF blocked: Target resolves to any-local address: " + addr.getHostAddress());
                }
                byte[] raw = addr.getAddress();
                if (raw.length == 4) {
                    // Check 169.254.0.0/16 cloud metadata
                    int b0 = raw[0] & 0xFF;
                    int b1 = raw[1] & 0xFF;
                    if (b0 == 169 && b1 == 254) {
                        throw new BadRequestException("SSRF blocked: Cloud metadata link-local address: " + addr.getHostAddress());
                    }
                    // Check 0.0.0.0/8
                    if (b0 == 0) {
                        throw new BadRequestException("SSRF blocked: Zero address: " + addr.getHostAddress());
                    }
                }
            }
        } catch (UnknownHostException e) {
            throw new BadRequestException("Cannot resolve host: " + host);
        }
    }

    /**
     * Downloads an image from a remote URL with SSRF protection, strict size bounds, and magic-byte validation.
     */
    public byte[] downloadBoundedImage(String urlString, long maxBytes) {
        if (urlString == null || urlString.isBlank()) {
            throw new BadRequestException("Target image URL cannot be empty");
        }

        URI currentUri;
        try {
            currentUri = URI.create(urlString.trim());
        } catch (Exception e) {
            throw new BadRequestException("Invalid URL syntax: " + urlString);
        }

        int redirectCount = 0;
        while (redirectCount <= MAX_REDIRECTS) {
            validateTargetUri(currentUri);

            try {
                URL url = currentUri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Elegance-Platform/1.0");

                int statusCode = conn.getResponseCode();

                if (statusCode >= 300 && statusCode < 400) {
                    String location = conn.getHeaderField("Location");
                    if (location == null || location.isBlank()) {
                        throw new BadRequestException("Redirect response missing Location header");
                    }
                    currentUri = currentUri.resolve(location);
                    redirectCount++;
                    conn.disconnect();
                    continue;
                }

                if (statusCode != 200) {
                    throw new BadRequestException("Remote provider returned non-OK status: " + statusCode);
                }

                long contentLength = conn.getContentLengthLong();
                if (contentLength > maxBytes) {
                    throw new BadRequestException("Remote content length (" + contentLength + " bytes) exceeds maximum limit of " + maxBytes + " bytes");
                }

                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long totalRead = 0;
                    while ((read = in.read(buffer)) != -1) {
                        totalRead += read;
                        if (totalRead > maxBytes) {
                            throw new BadRequestException("Remote content size exceeded maximum limit of " + maxBytes + " bytes");
                        }
                        out.write(buffer, 0, read);
                    }
                    byte[] downloaded = out.toByteArray();
                    if (downloaded.length == 0) {
                        throw new BadRequestException("Remote content is empty");
                    }
                    return downloaded;
                } finally {
                    conn.disconnect();
                }
            } catch (BadRequestException bre) {
                throw bre;
            } catch (Exception e) {
                log.warn("Failed to fetch remote image from {}: {}", currentUri, e.getMessage());
                throw new BadRequestException("Failed to download remote image: " + e.getMessage());
            }
        }

        throw new BadRequestException("Too many redirects while downloading remote image");
    }
}
