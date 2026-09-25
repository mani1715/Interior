package com.interior.platform.collections.repository;

import com.interior.platform.collections.domain.CollectionItemRecord;
import com.interior.platform.collections.domain.UserCollectionRecord;
import com.interior.platform.collections.dto.CollectionItemDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcUserCollectionRepository implements UserCollectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserCollectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UserCollectionRecord> collectionMapper = (rs, rowNum) -> new UserCollectionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "owner_user_id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getBoolean("is_default"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getLong("version")
    );

    private final RowMapper<CollectionItemRecord> itemMapper = (rs, rowNum) -> new CollectionItemRecord(
            getUuid(rs, "id"),
            getUuid(rs, "collection_id"),
            getUuid(rs, "owner_user_id"),
            getUuid(rs, "project_id"),
            rs.getString("note"),
            rs.getInt("display_order"),
            toInstant(rs.getTimestamp("created_at"))
    );

    @Override
    public UserCollectionRecord save(UserCollectionRecord collection) {
        String sql = """
            INSERT INTO user_collections (
                id, owner_user_id, title, description, is_default,
                created_at, updated_at, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                collection.id(),
                collection.ownerUserId(),
                collection.title(),
                collection.description(),
                collection.isDefault(),
                Timestamp.from(collection.createdAt()),
                Timestamp.from(collection.updatedAt()),
                collection.version()
        );
        return collection;
    }

    @Override
    public Optional<UserCollectionRecord> findById(UUID id) {
        String sql = "SELECT * FROM user_collections WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, collectionMapper, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserCollectionRecord> findDefaultCollection(UUID ownerUserId) {
        String sql = "SELECT * FROM user_collections WHERE owner_user_id = ? AND is_default = true";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, collectionMapper, ownerUserId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<UserCollectionRecord> listCollections(UUID ownerUserId) {
        String sql = "SELECT * FROM user_collections WHERE owner_user_id = ? ORDER BY is_default DESC, updated_at DESC";
        return jdbcTemplate.query(sql, collectionMapper, ownerUserId);
    }

    @Override
    public int countCollections(UUID ownerUserId) {
        String sql = "SELECT count(*) FROM user_collections WHERE owner_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ownerUserId);
        return count != null ? count : 0;
    }

    @Override
    public void updateCollection(UUID id, UUID ownerUserId, String title, String description) {
        String sql = """
            UPDATE user_collections
            SET title = ?, description = ?, updated_at = now(), version = version + 1
            WHERE id = ? AND owner_user_id = ?
        """;
        jdbcTemplate.update(sql, title, description, id, ownerUserId);
    }

    @Override
    public void deleteCollection(UUID id, UUID ownerUserId) {
        String sql = "DELETE FROM user_collections WHERE id = ? AND owner_user_id = ?";
        jdbcTemplate.update(sql, id, ownerUserId);
    }

    @Override
    public CollectionItemRecord saveItem(CollectionItemRecord item) {
        Optional<CollectionItemRecord> existing = findItemByProject(item.collectionId(), item.projectId());
        if (existing.isPresent()) {
            if (item.note() != null) {
                updateItemNote(existing.get().id(), item.ownerUserId(), item.note());
            }
            return findItemById(existing.get().id()).orElse(existing.get());
        }

        String sql = """
            INSERT INTO collection_items (
                id, collection_id, owner_user_id, project_id, note,
                display_order, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                item.id(),
                item.collectionId(),
                item.ownerUserId(),
                item.projectId(),
                item.note(),
                item.displayOrder(),
                Timestamp.from(item.createdAt())
        );
        return item;
    }

    @Override
    public Optional<CollectionItemRecord> findItemById(UUID itemId) {
        String sql = "SELECT * FROM collection_items WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, itemMapper, itemId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CollectionItemRecord> findItemByProject(UUID collectionId, UUID projectId) {
        String sql = "SELECT * FROM collection_items WHERE collection_id = ? AND project_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, itemMapper, collectionId, projectId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<CollectionItemRecord> listItems(UUID collectionId) {
        String sql = "SELECT * FROM collection_items WHERE collection_id = ? ORDER BY display_order ASC, created_at ASC";
        return jdbcTemplate.query(sql, itemMapper, collectionId);
    }

    @Override
    public List<CollectionItemDto> listItemsWithProjection(UUID collectionId) {
        String sql = """
            SELECT ci.id, ci.collection_id, ci.project_id, ci.note, ci.display_order, ci.created_at,
                   p.id AS actual_project_id, p.title AS p_title, p.slug AS p_slug,
                   p.project_status, p.visibility_status, p.archived_at,
                   s.name AS s_name, s.slug AS s_slug, s.publication_status, s.status AS s_status
            FROM collection_items ci
            LEFT JOIN studio_projects p ON ci.project_id = p.id
            LEFT JOIN designer_studios s ON p.studio_id = s.id
            WHERE ci.collection_id = ?
            ORDER BY ci.display_order ASC, ci.created_at ASC
        """;

        record RawItemRow(
                UUID id,
                UUID collectionId,
                UUID projectId,
                String note,
                int displayOrder,
                Instant createdAt,
                boolean isPublic,
                String projectTitle,
                String projectSlug,
                String studioName,
                String studioSlug
        ) {}

        List<RawItemRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            UUID actualProjectId = getUuid(rs, "actual_project_id");
            String projectStatus = rs.getString("project_status");
            String visibilityStatus = rs.getString("visibility_status");
            Timestamp archivedAt = rs.getTimestamp("archived_at");
            String pubStatus = rs.getString("publication_status");
            String studioStatus = rs.getString("s_status");

            boolean isPublic = actualProjectId != null
                    && "READY".equalsIgnoreCase(projectStatus)
                    && "PORTFOLIO".equalsIgnoreCase(visibilityStatus)
                    && archivedAt == null
                    && "PUBLISHED".equalsIgnoreCase(pubStatus)
                    && "ACTIVE".equalsIgnoreCase(studioStatus);

            return new RawItemRow(
                    getUuid(rs, "id"),
                    getUuid(rs, "collection_id"),
                    getUuid(rs, "project_id"),
                    rs.getString("note"),
                    rs.getInt("display_order"),
                    toInstant(rs.getTimestamp("created_at")),
                    isPublic,
                    isPublic ? rs.getString("p_title") : "This project is no longer available",
                    isPublic ? rs.getString("p_slug") : null,
                    isPublic ? rs.getString("s_name") : null,
                    isPublic ? rs.getString("s_slug") : null
            );
        }, collectionId);

        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> availableProjectIds = rows.stream()
                .filter(RawItemRow::isPublic)
                .map(RawItemRow::projectId)
                .distinct()
                .toList();

        Map<UUID, String> coversByProject = fetchCoversForProjects(availableProjectIds);

        List<CollectionItemDto> results = new ArrayList<>(rows.size());
        for (RawItemRow r : rows) {
            String coverUrl = r.isPublic() ? coversByProject.get(r.projectId()) : null;
            results.add(new CollectionItemDto(
                    r.id(),
                    r.collectionId(),
                    r.projectId(),
                    r.note(),
                    r.displayOrder(),
                    r.createdAt(),
                    r.isPublic(),
                    r.projectTitle(),
                    r.projectSlug(),
                    coverUrl,
                    r.studioName(),
                    r.studioSlug()
            ));
        }

        return results;
    }

    private Map<UUID, String> fetchCoversForProjects(List<UUID> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyMap();
        String inSql = String.join(",", Collections.nCopies(projectIds.size(), "?"));

        String sql = "SELECT ma.project_id, md.public_url, md.variant_name " +
                     "FROM media_assets ma " +
                     "JOIN media_derivatives md ON md.media_id = ma.id AND md.studio_id = ma.studio_id " +
                     "WHERE ma.project_id IN (" + inSql + ") " +
                     "AND ma.deleted_at IS NULL " +
                     "AND ma.visibility != 'PRIVATE' " +
                     "AND ma.media_type IN ('REAL_PROJECT', 'BEFORE', 'AFTER', 'AI_CONCEPT') " +
                     "ORDER BY ma.project_id, ma.is_cover DESC, ma.sort_order ASC";

        Map<UUID, Map<String, String>> variants = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            UUID pid = getUuid(rs, "project_id");
            String variant = rs.getString("variant_name");
            String url = rs.getString("public_url");

            if (!variants.containsKey(pid)) {
                variants.put(pid, new HashMap<>());
            }
            variants.get(pid).put(variant, url);
        }, projectIds.toArray());

        Map<UUID, String> out = new HashMap<>();
        for (Map.Entry<UUID, Map<String, String>> entry : variants.entrySet()) {
            UUID pid = entry.getKey();
            Map<String, String> v = entry.getValue();
            String chosen = v.get("MEDIUM");
            if (chosen == null) chosen = v.get("LARGE");
            if (chosen == null) chosen = v.get("THUMBNAIL");
            if (chosen == null && !v.isEmpty()) chosen = v.values().iterator().next();
            out.put(pid, chosen);
        }
        return out;
    }

    @Override
    public int countItems(UUID collectionId) {
        String sql = "SELECT count(*) FROM collection_items WHERE collection_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, collectionId);
        return count != null ? count : 0;
    }

    @Override
    public void deleteItem(UUID itemId, UUID ownerUserId) {
        String sql = "DELETE FROM collection_items WHERE id = ? AND owner_user_id = ?";
        jdbcTemplate.update(sql, itemId, ownerUserId);
    }

    @Override
    public void updateItemNote(UUID itemId, UUID ownerUserId, String note) {
        String sql = "UPDATE collection_items SET note = ? WHERE id = ? AND owner_user_id = ?";
        jdbcTemplate.update(sql, note, itemId, ownerUserId);
    }

    @Override
    public void updateItemDisplayOrder(UUID itemId, UUID collectionId, int displayOrder) {
        String sql = "UPDATE collection_items SET display_order = ? WHERE id = ? AND collection_id = ?";
        jdbcTemplate.update(sql, displayOrder, itemId, collectionId);
    }

    private static UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val instanceof UUID u) return u;
        if (val instanceof String s) return UUID.fromString(s);
        return null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
