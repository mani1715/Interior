package com.interior.platform.privacy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.media.storage.StorageService;
import com.interior.platform.privacy.domain.*;
import com.interior.platform.privacy.dto.DataExportResponse;
import com.interior.platform.privacy.repository.PrivacyRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PrivacyExportService {

    private final PrivacyRepository privacyRepository;
    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public PrivacyExportService(
            PrivacyRepository privacyRepository,
            JdbcTemplate jdbcTemplate,
            StorageService storageService,
            ObjectMapper objectMapper
    ) {
        this.privacyRepository = privacyRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    public static String sanitizeCsvField(String field) {
        if (field == null) {
            return "";
        }
        String trimmed = field.trim();
        // CSV Injection defense: prepending single quote if starts with formula symbols
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            trimmed = "'" + trimmed;
        }
        if (trimmed.contains(",") || trimmed.contains("\"") || trimmed.contains("\n") || trimmed.contains("\r")) {
            return "\"" + trimmed.replace("\"", "\"\"") + "\"";
        }
        return trimmed;
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public DataExportResponse triggerUserExport(UUID userId) {
        UUID requestId = UuidV7.randomUuid();
        PrivacyRequestRecord req = privacyRepository.createPrivacyRequest(
                requestId,
                userId,
                null,
                PrivacyRequestType.USER_DATA_EXPORT,
                PrivacyRequestStatus.PROCESSING,
                Instant.now(),
                "{}"
        );

        UUID jobId = UuidV7.randomUuid();
        DataExportJobRecord job = privacyRepository.createDataExportJob(
                jobId,
                requestId,
                userId,
                null,
                ExportType.USER,
                DataExportStatus.PROCESSING
        );

        try {
            byte[] zipData = compileUserDataPackage(userId);
            String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            String tokenHash = hashToken(rawToken);
            String storageKey = "exports/users/" + userId + "/" + jobId + ".zip";

            storageService.store(storageKey, zipData, "application/zip");
            Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

            privacyRepository.updateDataExportJobReady(jobId, storageKey, zipData.length, tokenHash, expiresAt);
            privacyRepository.updatePrivacyRequestStatus(requestId, PrivacyRequestStatus.COMPLETED, Instant.now(), null);

            return new DataExportResponse(
                    jobId,
                    requestId,
                    ExportType.USER.name(),
                    DataExportStatus.READY.name(),
                    (long) zipData.length,
                    rawToken,
                    "/privacy/export/download/" + jobId + "?token=" + rawToken,
                    expiresAt,
                    0,
                    Instant.now()
            );
        } catch (Exception e) {
            privacyRepository.updateDataExportJobStatus(jobId, DataExportStatus.FAILED);
            privacyRepository.updatePrivacyRequestStatus(requestId, PrivacyRequestStatus.FAILED, Instant.now(), e.getMessage());
            throw new RuntimeException("Failed to generate user data export: " + e.getMessage(), e);
        }
    }

    public DataExportResponse triggerStudioExport(UUID studioId, UUID actorUserId) {
        UUID requestId = UuidV7.randomUuid();
        PrivacyRequestRecord req = privacyRepository.createPrivacyRequest(
                requestId,
                actorUserId,
                studioId,
                PrivacyRequestType.STUDIO_DATA_EXPORT,
                PrivacyRequestStatus.PROCESSING,
                Instant.now(),
                "{}"
        );

        UUID jobId = UuidV7.randomUuid();
        DataExportJobRecord job = privacyRepository.createDataExportJob(
                jobId,
                requestId,
                actorUserId,
                studioId,
                ExportType.STUDIO,
                DataExportStatus.PROCESSING
        );

        try {
            byte[] zipData = compileStudioDataPackage(studioId);
            String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            String tokenHash = hashToken(rawToken);
            String storageKey = "exports/studios/" + studioId + "/" + jobId + ".zip";

            storageService.store(storageKey, zipData, "application/zip");
            Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

            privacyRepository.updateDataExportJobReady(jobId, storageKey, zipData.length, tokenHash, expiresAt);
            privacyRepository.updatePrivacyRequestStatus(requestId, PrivacyRequestStatus.COMPLETED, Instant.now(), null);

            return new DataExportResponse(
                    jobId,
                    requestId,
                    ExportType.STUDIO.name(),
                    DataExportStatus.READY.name(),
                    (long) zipData.length,
                    rawToken,
                    "/privacy/export/download/" + jobId + "?token=" + rawToken,
                    expiresAt,
                    0,
                    Instant.now()
            );
        } catch (Exception e) {
            privacyRepository.updateDataExportJobStatus(jobId, DataExportStatus.FAILED);
            privacyRepository.updatePrivacyRequestStatus(requestId, PrivacyRequestStatus.FAILED, Instant.now(), e.getMessage());
            throw new RuntimeException("Failed to generate studio data export: " + e.getMessage(), e);
        }
    }

    private byte[] compileUserDataPackage(UUID userId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 1. Profile JSON (Strictly sanitized: no tokens, hashes or internal secrets)
            Map<String, Object> userProfile = jdbcTemplate.queryForMap(
                    "SELECT id, display_name, email, phone, avatar_url, status, created_at, updated_at FROM users WHERE id = ?",
                    userId
            );
            addZipEntry(zos, "profile.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(userProfile));

            // 2. Collections / Moodboards JSON
            List<Map<String, Object>> collections = jdbcTemplate.queryForList(
                    "SELECT id, name, description, is_private, created_at, updated_at FROM user_collections WHERE user_id = ?",
                    userId
            );
            addZipEntry(zos, "collections.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(collections));

            // 3. Customer Inquiries CSV
            List<Map<String, Object>> inquiries = jdbcTemplate.queryForList(
                    "SELECT id, studio_id, service_type, message, status, created_at FROM studio_leads WHERE customer_user_id = ? ORDER BY created_at DESC",
                    userId
            );
            StringBuilder inquiryCsv = new StringBuilder("id,studio_id,service_type,message,status,created_at\n");
            for (Map<String, Object> row : inquiries) {
                inquiryCsv.append(sanitizeCsvField(String.valueOf(row.get("id")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("studio_id")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("service_type")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("message")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("status")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("created_at")))).append("\n");
            }
            addZipEntry(zos, "customer_inquiries.csv", inquiryCsv.toString().getBytes(StandardCharsets.UTF_8));

            // 4. Manifest JSON
            Map<String, Object> manifest = Map.of(
                    "export_type", "USER_DATA_EXPORT",
                    "user_id", userId.toString(),
                    "generated_at", Instant.now().toString(),
                    "contents", List.of("profile.json", "collections.json", "customer_inquiries.csv"),
                    "redacted_fields", List.of("passwords", "tokens", "mfa_secrets", "recovery_codes", "admin_internal_notes")
            );
            addZipEntry(zos, "manifest.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
        }
        return baos.toByteArray();
    }

    private byte[] compileStudioDataPackage(UUID studioId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 1. Studio Business Profile
            Map<String, Object> studio = jdbcTemplate.queryForMap(
                    "SELECT id, name, slug, tagline, description, publication_status, verification_status, status, created_at, updated_at FROM designer_studios WHERE id = ?",
                    studioId
            );
            addZipEntry(zos, "studio_profile.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(studio));

            // 2. Studio Projects JSON
            List<Map<String, Object>> projects = jdbcTemplate.queryForList(
                    "SELECT id, title, slug, project_type, style, location, budget_tier, project_status, visibility_status, created_at FROM studio_projects WHERE studio_id = ?",
                    studioId
            );
            addZipEntry(zos, "projects.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(projects));

            // 3. Studio Team Members CSV
            List<Map<String, Object>> team = jdbcTemplate.queryForList(
                    "SELECT sm.id, sm.role, u.display_name, u.email, sm.granted_at FROM studio_members sm JOIN users u ON sm.user_id = u.id WHERE sm.studio_id = ?",
                    studioId
            );
            StringBuilder teamCsv = new StringBuilder("membership_id,role,display_name,email,granted_at\n");
            for (Map<String, Object> row : team) {
                teamCsv.append(sanitizeCsvField(String.valueOf(row.get("id")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("role")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("display_name")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("email")))).append(",")
                        .append(sanitizeCsvField(String.valueOf(row.get("granted_at")))).append("\n");
            }
            addZipEntry(zos, "team_members.csv", teamCsv.toString().getBytes(StandardCharsets.UTF_8));

            // 4. Studio Manifest
            Map<String, Object> manifest = Map.of(
                    "export_type", "STUDIO_DATA_EXPORT",
                    "studio_id", studioId.toString(),
                    "generated_at", Instant.now().toString(),
                    "contents", List.of("studio_profile.json", "projects.json", "team_members.csv"),
                    "redacted_fields", List.of("mfa_secrets", "api_tokens", "raw_billing_secrets", "internal_moderator_notes")
            );
            addZipEntry(zos, "manifest.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
        }
        return baos.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zos, String entryName, byte[] data) throws Exception {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(data);
        zos.closeEntry();
    }

    public byte[] downloadExport(UUID jobId, String token) {
        DataExportJobRecord job = privacyRepository.findDataExportJobById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Data export job not found"));

        if (job.status() != DataExportStatus.READY) {
            throw new IllegalStateException("Data export is not ready (status: " + job.status() + ")");
        }

        if (job.expiresAt() != null && Instant.now().isAfter(job.expiresAt())) {
            privacyRepository.updateDataExportJobStatus(jobId, DataExportStatus.EXPIRED);
            throw new IllegalStateException("Data export link has expired");
        }

        String computedHash = hashToken(token);
        if (!computedHash.equals(job.downloadTokenHash())) {
            throw new SecurityException("Invalid download token");
        }

        if (job.storageKey() == null || !storageService.exists(job.storageKey())) {
            throw new IllegalStateException("Export package not found in storage");
        }

        privacyRepository.incrementDownloadCount(jobId);
        return storageService.load(job.storageKey());
    }
}
