package com.interior.platform.designers.repository;

import com.interior.platform.designers.domain.OnboardingDraftRecord;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.domain.StudioSpecialtyRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcStudioRepository implements StudioRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcStudioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveDraft(UUID userId, int step, String draftPayload, String status) {
        String updateSql = "UPDATE designer_onboarding_drafts SET step = ?, draft_payload = ?, status = ?, updated_at = now() WHERE user_id = ?";
        int updated = jdbcTemplate.update(updateSql, step, draftPayload, status, userId);
        if (updated == 0) {
            String insertSql = "INSERT INTO designer_onboarding_drafts (id, user_id, step, draft_payload, status, created_at, updated_at) " +
                               "VALUES (?, ?, ?, ?, ?, now(), now())";
            jdbcTemplate.update(insertSql, UUID.randomUUID(), userId, step, draftPayload, status);
        }
    }

    @Override
    public Optional<OnboardingDraftRecord> findDraftByUserId(UUID userId) {
        String sql = "SELECT id, user_id, step, draft_payload, status, created_at, updated_at " +
                     "FROM designer_onboarding_drafts WHERE user_id = ?";
        List<OnboardingDraftRecord> list = jdbcTemplate.query(sql, (rs, rowNum) -> new OnboardingDraftRecord(
                getUuid(rs, "id"),
                getUuid(rs, "user_id"),
                rs.getInt("step"),
                rs.getString("draft_payload"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ), userId);
        return list.stream().findFirst();
    }

    @Override
    public void markDraftCompleted(UUID userId) {
        String sql = "UPDATE designer_onboarding_drafts SET status = 'COMPLETED', updated_at = now() WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    @Override
    public boolean isSlugClaimed(String slug) {
        String sql = "SELECT COUNT(1) FROM studio_slug_claims WHERE lower(slug) = lower(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, slug.trim());
        return count != null && count > 0;
    }

    @Override
    public void claimSlug(UUID studioId, String slug, String state) {
        String sql = "INSERT INTO studio_slug_claims (id, studio_id, slug, state, created_at) " +
                     "VALUES (?, ?, ?, ?, now())";
        jdbcTemplate.update(sql, UUID.randomUUID(), studioId, slug.trim().toLowerCase(), state);
    }

    @Override
    public void createStudio(StudioDetailRecord studio) {
        String sql = "INSERT INTO designer_studios (" +
                     "id, name, slug, owner_id, status, professional_type, professional_title, tagline, " +
                     "experience_since_year, team_size, budget_range, address_line, city, district, state, " +
                     "postal_code, country, travel_available, gst_registered, gst_number, publication_status, " +
                     "onboarding_completed_at, created_at, updated_at, version" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), 0)";

        jdbcTemplate.update(sql,
                studio.id(),
                studio.name(),
                studio.slug().toLowerCase(),
                studio.ownerId(),
                studio.status(),
                studio.professionalType(),
                studio.professionalTitle(),
                studio.tagline(),
                studio.experienceSinceYear(),
                studio.teamSize(),
                studio.budgetRange(),
                studio.addressLine(),
                studio.city(),
                studio.district(),
                studio.state(),
                studio.postalCode(),
                studio.country() != null ? studio.country() : "IN",
                studio.travelAvailable(),
                studio.gstRegistered(),
                studio.gstNumber(),
                studio.publicationStatus() != null ? studio.publicationStatus() : "UNPUBLISHED",
                studio.onboardingCompletedAt() != null ? Timestamp.from(studio.onboardingCompletedAt()) : Timestamp.from(Instant.now())
        );
    }

    @Override
    public void addStudioContact(UUID studioId, String kind, String value, boolean publicConsent, int sortOrder) {
        String sql = "INSERT INTO studio_contacts (id, studio_id, kind, contact_value, public_consent, sort_order) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, UUID.randomUUID(), studioId, kind, value, publicConsent, sortOrder);
    }

    @Override
    public void addStudioService(UUID studioId, String serviceCode, String serviceName) {
        String sql = "INSERT INTO studio_services (id, studio_id, service_code, service_name) " +
                     "VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, UUID.randomUUID(), studioId, serviceCode, serviceName);
    }

    @Override
    public void addStudioSpecialty(UUID studioId, String specialtyCode, String specialtyName) {
        String sql = "INSERT INTO studio_specialties (id, studio_id, specialty_code, specialty_name) " +
                     "VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, UUID.randomUUID(), studioId, specialtyCode, specialtyName);
    }

    @Override
    public List<StudioSpecialtyRecord> getStudioSpecialties(UUID studioId) {
        String sql = "SELECT id, studio_id, specialty_code, specialty_name FROM studio_specialties WHERE studio_id = ? ORDER BY specialty_name ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudioSpecialtyRecord(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("specialty_code"),
                rs.getString("specialty_name")
        ), studioId);
    }

    @Override
    public void addStudioServiceArea(UUID studioId, String cityName, String locality) {
        String sql = "INSERT INTO studio_service_areas (id, studio_id, city_name, locality) " +
                     "VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, UUID.randomUUID(), studioId, cityName, locality);
    }

    @Override
    public Optional<UUID> findInitialOnboardingStudioId(UUID userId) {
        String sql = "SELECT studio_id FROM designer_onboarding_completions WHERE user_id = ?";
        List<UUID> list = jdbcTemplate.query(sql, (rs, rowNum) -> getUuid(rs, "studio_id"), userId);
        return list.stream().findFirst();
    }

    @Override
    public void recordInitialOnboardingCompletion(UUID userId, UUID studioId) {
        String sql = "INSERT INTO designer_onboarding_completions (user_id, studio_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, userId, studioId);
    }

    @Override
    public Optional<StudioDetailRecord> findStudioById(UUID studioId) {
        String sql = "SELECT * FROM designer_studios WHERE id = ?";
        return queryStudio(sql, studioId);
    }

    @Override
    public Optional<StudioDetailRecord> findStudioByOwnerId(UUID ownerId) {
        String sql = "SELECT * FROM designer_studios WHERE owner_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1";
        return queryStudio(sql, ownerId);
    }

    @Override
    public Optional<StudioDetailRecord> findStudioBySlug(String slug) {
        String sql = "SELECT * FROM designer_studios WHERE lower(slug) = lower(?)";
        return queryStudio(sql, slug);
    }

    @Override
    public boolean hasCompletedOnboarding(UUID userId) {
        String sql = "SELECT COUNT(1) FROM designer_onboarding_completions WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    private Optional<StudioDetailRecord> queryStudio(String sql, Object... params) {
        List<StudioDetailRecord> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID id = getUuid(rs, "id");
            List<StudioDetailRecord.StudioContactItem> contacts = queryContacts(id);
            List<StudioDetailRecord.StudioServiceItem> services = queryServices(id);
            List<StudioDetailRecord.StudioSpecialtyItem> specialties = querySpecialtyItems(id);
            List<StudioDetailRecord.StudioServiceAreaItem> serviceAreas = queryServiceAreas(id);

            Integer expYear = rs.getObject("experience_since_year") != null ? rs.getInt("experience_since_year") : null;
            Timestamp completedTs = rs.getTimestamp("onboarding_completed_at");

            return new StudioDetailRecord(
                    id,
                    rs.getString("name"),
                    rs.getString("slug"),
                    getUuid(rs, "owner_id"),
                    rs.getString("status"),
                    rs.getString("professional_type"),
                    rs.getString("professional_title"),
                    rs.getString("tagline"),
                    expYear,
                    rs.getString("team_size"),
                    rs.getString("budget_range"),
                    rs.getString("address_line"),
                    rs.getString("city"),
                    rs.getString("district"),
                    rs.getString("state"),
                    rs.getString("postal_code"),
                    rs.getString("country"),
                    rs.getBoolean("travel_available"),
                    rs.getBoolean("gst_registered"),
                    rs.getString("gst_number"),
                    rs.getString("publication_status"),
                    completedTs != null ? completedTs.toInstant() : null,
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    contacts,
                    services,
                    specialties,
                    serviceAreas
            );
        }, params);
        return list.stream().findFirst();
    }

    private List<StudioDetailRecord.StudioSpecialtyItem> querySpecialtyItems(UUID studioId) {
        String sql = "SELECT specialty_code, specialty_name FROM studio_specialties WHERE studio_id = ? ORDER BY specialty_name ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudioDetailRecord.StudioSpecialtyItem(
                rs.getString("specialty_code"),
                rs.getString("specialty_name")
        ), studioId);
    }

    private List<StudioDetailRecord.StudioContactItem> queryContacts(UUID studioId) {
        String sql = "SELECT kind, contact_value, public_consent, sort_order FROM studio_contacts WHERE studio_id = ? ORDER BY sort_order ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudioDetailRecord.StudioContactItem(
                rs.getString("kind"),
                rs.getString("contact_value"),
                rs.getBoolean("public_consent"),
                rs.getInt("sort_order")
        ), studioId);
    }

    private List<StudioDetailRecord.StudioServiceItem> queryServices(UUID studioId) {
        String sql = "SELECT service_code, service_name FROM studio_services WHERE studio_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudioDetailRecord.StudioServiceItem(
                rs.getString("service_code"),
                rs.getString("service_name")
        ), studioId);
    }

    private List<StudioDetailRecord.StudioServiceAreaItem> queryServiceAreas(UUID studioId) {
        String sql = "SELECT city_name, locality FROM studio_service_areas WHERE studio_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new StudioDetailRecord.StudioServiceAreaItem(
                rs.getString("city_name"),
                rs.getString("locality")
        ), studioId);
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object obj = rs.getObject(column);
        if (obj == null) return null;
        if (obj instanceof UUID uuid) return uuid;
        return UUID.fromString(obj.toString());
    }
}
