package com.interior.platform.leads.repository;

import com.interior.platform.leads.domain.*;
import com.interior.platform.leads.dto.LeadCountsDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLeadRepository implements LeadRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLeadRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public StudioLeadRecord save(StudioLeadRecord lead) {
        String sql = "INSERT INTO studio_leads (" +
                "id, studio_id, project_id, source, status, name, " +
                "phone_normalized, email_normalized, city, project_category, " +
                "budget_range, message, preferred_contact_channel, " +
                "contact_consent_at, whatsapp_consent_at, assigned_user_id, " +
                "next_follow_up_at, lost_reason, possible_duplicate, idempotency_key, " +
                "created_at, updated_at, version, archived_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                lead.id(),
                lead.studioId(),
                lead.projectId(),
                lead.source().name(),
                lead.status().name(),
                lead.name(),
                lead.phoneNormalized(),
                lead.emailNormalized(),
                lead.city(),
                lead.projectCategory(),
                lead.budgetRange(),
                lead.message(),
                lead.preferredContactChannel() != null ? lead.preferredContactChannel().name() : null,
                lead.contactConsentAt() != null ? Timestamp.from(lead.contactConsentAt()) : Timestamp.from(Instant.now()),
                lead.whatsappConsentAt() != null ? Timestamp.from(lead.whatsappConsentAt()) : null,
                lead.assignedUserId(),
                lead.nextFollowUpAt() != null ? Timestamp.from(lead.nextFollowUpAt()) : null,
                lead.lostReason(),
                lead.possibleDuplicate(),
                lead.idempotencyKey(),
                lead.createdAt() != null ? Timestamp.from(lead.createdAt()) : Timestamp.from(Instant.now()),
                lead.updatedAt() != null ? Timestamp.from(lead.updatedAt()) : Timestamp.from(Instant.now()),
                lead.version(),
                lead.archivedAt() != null ? Timestamp.from(lead.archivedAt()) : null
        );

        return lead;
    }

    @Override
    public Optional<StudioLeadRecord> findById(UUID studioId, UUID leadId) {
        String sql = "SELECT * FROM studio_leads WHERE studio_id = ? AND id = ?";
        List<StudioLeadRecord> results = jdbcTemplate.query(sql, leadRowMapper(), studioId, leadId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public boolean update(StudioLeadRecord lead, long expectedVersion) {
        String sql = "UPDATE studio_leads SET " +
                "status = ?, " +
                "lost_reason = ?, " +
                "next_follow_up_at = ?, " +
                "assigned_user_id = ?, " +
                "updated_at = now(), " +
                "version = version + 1 " +
                "WHERE id = ? AND studio_id = ? AND version = ?";

        int rows = jdbcTemplate.update(sql,
                lead.status().name(),
                lead.lostReason(),
                lead.nextFollowUpAt() != null ? Timestamp.from(lead.nextFollowUpAt()) : null,
                lead.assignedUserId(),
                lead.id(),
                lead.studioId(),
                expectedVersion
        );

        return rows > 0;
    }

    @Override
    public boolean archive(UUID studioId, UUID leadId, long expectedVersion) {
        String sql = "UPDATE studio_leads SET " +
                "status = 'ARCHIVED', " +
                "archived_at = now(), " +
                "updated_at = now(), " +
                "version = version + 1 " +
                "WHERE id = ? AND studio_id = ? AND version = ?";

        int rows = jdbcTemplate.update(sql, leadId, studioId, expectedVersion);
        return rows > 0;
    }

    @Override
    public List<StudioLeadRecord> listLeads(
            UUID studioId,
            LeadStatus status,
            UUID assignedUserId,
            String search,
            String sort,
            int limit,
            int offset
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT l.* FROM studio_leads l " +
                "LEFT JOIN studio_projects p ON p.id = l.project_id " +
                "WHERE l.studio_id = ? "
        );
        List<Object> args = new ArrayList<>();
        args.add(studioId);

        applyFilters(sql, args, status, assignedUserId, search);

        // Sorting
        if ("oldest".equalsIgnoreCase(sort)) {
            sql.append("ORDER BY l.created_at ASC, l.id ASC ");
        } else if ("follow_up".equalsIgnoreCase(sort)) {
            sql.append("ORDER BY l.next_follow_up_at ASC NULLS LAST, l.created_at DESC, l.id DESC ");
        } else if ("updated".equalsIgnoreCase(sort)) {
            sql.append("ORDER BY l.updated_at DESC, l.id DESC ");
        } else {
            // Default newest
            sql.append("ORDER BY l.created_at DESC, l.id DESC ");
        }

        sql.append("LIMIT ? OFFSET ?");
        args.add(Math.max(1, Math.min(100, limit)));
        args.add(Math.max(0, offset));

        return jdbcTemplate.query(sql.toString(), leadRowMapper(), args.toArray());
    }

    @Override
    public long countLeads(UUID studioId, LeadStatus status, UUID assignedUserId, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM studio_leads l " +
                "LEFT JOIN studio_projects p ON p.id = l.project_id " +
                "WHERE l.studio_id = ? "
        );
        List<Object> args = new ArrayList<>();
        args.add(studioId);

        applyFilters(sql, args, status, assignedUserId, search);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count != null ? count : 0L;
    }

    private void applyFilters(StringBuilder sql, List<Object> args, LeadStatus status, UUID assignedUserId, String search) {
        if (status != null) {
            sql.append("AND l.status = ? ");
            args.add(status.name());
        } else {
            sql.append("AND l.archived_at IS NULL ");
        }

        if (assignedUserId != null) {
            sql.append("AND l.assigned_user_id = ? ");
            args.add(assignedUserId);
        }

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(l.name) LIKE ? OR l.phone_normalized LIKE ? OR LOWER(l.email_normalized) LIKE ? OR LOWER(p.title) LIKE ?) ");
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
            args.add(pattern);
        }
    }

    @Override
    public LeadCountsDto getCounts(UUID studioId) {
        String sql = "SELECT " +
                "COUNT(CASE WHEN archived_at IS NULL THEN 1 END) as total, " +
                "COUNT(CASE WHEN status = 'NEW' AND archived_at IS NULL THEN 1 END) as new_leads, " +
                "COUNT(CASE WHEN status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'SITE_VISIT_PLANNED', 'IN_DISCUSSION') AND archived_at IS NULL THEN 1 END) as active, " +
                "COUNT(CASE WHEN status = 'WON' AND archived_at IS NULL THEN 1 END) as won, " +
                "COUNT(CASE WHEN status = 'LOST' AND archived_at IS NULL THEN 1 END) as lost, " +
                "COUNT(CASE WHEN status = 'ARCHIVED' OR archived_at IS NOT NULL THEN 1 END) as archived " +
                "FROM studio_leads WHERE studio_id = ?";

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new LeadCountsDto(
                rs.getLong("total"),
                rs.getLong("new_leads"),
                rs.getLong("active"),
                rs.getLong("won"),
                rs.getLong("lost"),
                rs.getLong("archived")
        ), studioId);
    }

    @Override
    public Optional<StudioLeadRecord> findRecentDuplicate(UUID studioId, String phoneNormalized, String emailNormalized, Instant since) {
        String sql = "SELECT * FROM studio_leads WHERE studio_id = ? AND created_at >= ? AND (" +
                "phone_normalized = ? OR (email_normalized IS NOT NULL AND email_normalized = ?)" +
                ") ORDER BY created_at DESC LIMIT 1";

        List<StudioLeadRecord> results = jdbcTemplate.query(
                sql,
                leadRowMapper(),
                studioId,
                Timestamp.from(since),
                phoneNormalized,
                emailNormalized != null ? emailNormalized : ""
        );

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<StudioLeadRecord> findByIdempotencyKey(UUID studioId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM studio_leads WHERE studio_id = ? AND idempotency_key = ? LIMIT 1";
        List<StudioLeadRecord> results = jdbcTemplate.query(sql, leadRowMapper(), studioId, idempotencyKey.trim());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void saveActivity(LeadActivityRecord activity) {
        String sql = "INSERT INTO lead_activities (id, lead_id, studio_id, actor_id, activity_type, details, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                activity.id(),
                activity.leadId(),
                activity.studioId(),
                activity.actorId(),
                activity.activityType().name(),
                activity.details(),
                activity.createdAt() != null ? Timestamp.from(activity.createdAt()) : Timestamp.from(Instant.now())
        );
    }

    @Override
    public List<LeadActivityRecord> listActivities(UUID studioId, UUID leadId) {
        String sql = "SELECT * FROM lead_activities WHERE studio_id = ? AND lead_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LeadActivityRecord(
                getUuid(rs, "id"),
                getUuid(rs, "lead_id"),
                getUuid(rs, "studio_id"),
                getUuid(rs, "actor_id"),
                LeadActivityType.valueOf(rs.getString("activity_type")),
                rs.getString("details"),
                getInstant(rs, "created_at")
        ), studioId, leadId);
    }

    @Override
    public void saveNote(LeadNoteRecord note) {
        String sql = "INSERT INTO lead_notes (id, lead_id, studio_id, author_id, content, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                note.id(),
                note.leadId(),
                note.studioId(),
                note.authorId(),
                note.content(),
                note.createdAt() != null ? Timestamp.from(note.createdAt()) : Timestamp.from(Instant.now())
        );
    }

    @Override
    public List<LeadNoteRecord> listNotes(UUID studioId, UUID leadId) {
        String sql = "SELECT * FROM lead_notes WHERE studio_id = ? AND lead_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LeadNoteRecord(
                getUuid(rs, "id"),
                getUuid(rs, "lead_id"),
                getUuid(rs, "studio_id"),
                getUuid(rs, "author_id"),
                rs.getString("content"),
                getInstant(rs, "created_at")
        ), studioId, leadId);
    }

    @Override
    public void saveWhatsAppMessage(LeadWhatsAppMessageRecord message) {
        String sql = "INSERT INTO lead_whatsapp_messages (" +
                "id, studio_id, lead_id, direction, provider, provider_message_id, status, body, failure_code, created_at, status_updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                message.id(),
                message.studioId(),
                message.leadId(),
                message.direction().name(),
                message.provider(),
                message.providerMessageId(),
                message.status().name(),
                message.body(),
                message.failureCode(),
                message.createdAt() != null ? Timestamp.from(message.createdAt()) : Timestamp.from(Instant.now()),
                message.statusUpdatedAt() != null ? Timestamp.from(message.statusUpdatedAt()) : Timestamp.from(Instant.now())
        );
    }

    @Override
    public boolean updateWhatsAppMessageStatus(UUID studioId, UUID messageId, WhatsAppMessageStatus status, String failureCode) {
        String sql = "UPDATE lead_whatsapp_messages SET status = ?, failure_code = ?, status_updated_at = now() " +
                "WHERE id = ? AND studio_id = ?";
        return jdbcTemplate.update(sql, status.name(), failureCode, messageId, studioId) > 0;
    }

    @Override
    public Optional<LeadWhatsAppMessageRecord> findWhatsAppMessageByProviderId(String provider, String providerMessageId) {
        String sql = "SELECT * FROM lead_whatsapp_messages WHERE provider = ? AND provider_message_id = ? LIMIT 1";
        List<LeadWhatsAppMessageRecord> results = jdbcTemplate.query(sql, whatsappMessageRowMapper(), provider, providerMessageId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<LeadWhatsAppMessageRecord> listWhatsAppMessages(UUID studioId, UUID leadId) {
        String sql = "SELECT * FROM lead_whatsapp_messages WHERE studio_id = ? AND lead_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, whatsappMessageRowMapper(), studioId, leadId);
    }

    @Override
    public Optional<PublicStudioTarget> findPublicStudioBySlug(String slug) {
        String sql = "SELECT id, name, slug FROM designer_studios " +
                "WHERE slug = ? AND publication_status = 'PUBLISHED' AND status = 'ACTIVE'";
        List<PublicStudioTarget> results = jdbcTemplate.query(sql, (rs, rowNum) -> new PublicStudioTarget(
                getUuid(rs, "id"),
                rs.getString("name"),
                rs.getString("slug")
        ), slug);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<PublicStudioTarget> findPublicStudioById(UUID studioId) {
        String sql = "SELECT id, name, slug FROM designer_studios " +
                "WHERE id = ? AND publication_status = 'PUBLISHED' AND status = 'ACTIVE'";
        List<PublicStudioTarget> results = jdbcTemplate.query(sql, (rs, rowNum) -> new PublicStudioTarget(
                getUuid(rs, "id"),
                rs.getString("name"),
                rs.getString("slug")
        ), studioId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<PublicProjectTarget> findPublicProjectBySlug(UUID studioId, String projectSlug) {
        String sql = "SELECT id, studio_id, title, slug FROM studio_projects " +
                "WHERE studio_id = ? AND slug = ? AND project_status = 'READY' " +
                "AND visibility_status = 'PORTFOLIO' AND archived_at IS NULL";
        List<PublicProjectTarget> results = jdbcTemplate.query(sql, (rs, rowNum) -> new PublicProjectTarget(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("title"),
                rs.getString("slug")
        ), studioId, projectSlug);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<PublicProjectTarget> findPublicProjectById(UUID studioId, UUID projectId) {
        String sql = "SELECT id, studio_id, title, slug FROM studio_projects " +
                "WHERE studio_id = ? AND id = ? AND project_status = 'READY' " +
                "AND visibility_status = 'PORTFOLIO' AND archived_at IS NULL";
        List<PublicProjectTarget> results = jdbcTemplate.query(sql, (rs, rowNum) -> new PublicProjectTarget(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("title"),
                rs.getString("slug")
        ), studioId, projectId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<PublicWhatsAppContact> findPublicWhatsAppContact(UUID studioId) {
        String sql = "SELECT id, studio_id, contact_value FROM studio_contacts " +
                "WHERE studio_id = ? AND kind = 'WHATSAPP' AND public_consent = true " +
                "ORDER BY sort_order ASC LIMIT 1";
        List<PublicWhatsAppContact> results = jdbcTemplate.query(sql, (rs, rowNum) -> new PublicWhatsAppContact(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                rs.getString("contact_value")
        ), studioId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public boolean isStudioMember(UUID studioId, UUID userId) {
        String sql = "SELECT COUNT(*) FROM studio_members sm " +
                     "JOIN users u ON u.id = sm.user_id " +
                     "WHERE sm.studio_id = ? AND sm.user_id = ? AND u.status = 'ACTIVE'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studioId, userId);
        return count != null && count > 0;
    }

    @Override
    public String getUserDisplayName(UUID userId) {
        if (userId == null) return null;
        String sql = "SELECT display_name FROM users WHERE id = ?";
        List<String> names = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("display_name"), userId);
        return names.isEmpty() ? null : names.get(0);
    }

    @Override
    public String getProjectTitle(UUID projectId) {
        if (projectId == null) return null;
        String sql = "SELECT title FROM studio_projects WHERE id = ?";
        List<String> titles = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("title"), projectId);
        return titles.isEmpty() ? null : titles.get(0);
    }

    private RowMapper<StudioLeadRecord> leadRowMapper() {
        return (rs, rowNum) -> new StudioLeadRecord(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                getUuid(rs, "project_id"),
                LeadSource.valueOf(rs.getString("source")),
                LeadStatus.valueOf(rs.getString("status")),
                rs.getString("name"),
                rs.getString("phone_normalized"),
                rs.getString("email_normalized"),
                rs.getString("city"),
                rs.getString("project_category"),
                rs.getString("budget_range"),
                rs.getString("message"),
                rs.getString("preferred_contact_channel") != null ? PreferredContactChannel.valueOf(rs.getString("preferred_contact_channel")) : null,
                getInstant(rs, "contact_consent_at"),
                getInstant(rs, "whatsapp_consent_at"),
                getUuid(rs, "assigned_user_id"),
                getInstant(rs, "next_follow_up_at"),
                rs.getString("lost_reason"),
                rs.getBoolean("possible_duplicate"),
                rs.getString("idempotency_key"),
                getInstant(rs, "created_at"),
                getInstant(rs, "updated_at"),
                rs.getLong("version"),
                getInstant(rs, "archived_at")
        );
    }

    private RowMapper<LeadWhatsAppMessageRecord> whatsappMessageRowMapper() {
        return (rs, rowNum) -> new LeadWhatsAppMessageRecord(
                getUuid(rs, "id"),
                getUuid(rs, "studio_id"),
                getUuid(rs, "lead_id"),
                WhatsAppDirection.valueOf(rs.getString("direction")),
                rs.getString("provider"),
                rs.getString("provider_message_id"),
                WhatsAppMessageStatus.valueOf(rs.getString("status")),
                rs.getString("body"),
                rs.getString("failure_code"),
                getInstant(rs, "created_at"),
                getInstant(rs, "status_updated_at")
        );
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    private Instant getInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }
}
