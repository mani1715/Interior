package com.interior.platform.billing.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.billing.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcBillingRepository implements BillingRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcBillingRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private Boolean isPostgres;

    public JdbcBillingRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private synchronized boolean isPostgreSql() {
        if (isPostgres == null) {
            try {
                String dbProduct = jdbcTemplate.execute((java.sql.Connection conn) -> conn.getMetaData().getDatabaseProductName());
                isPostgres = dbProduct != null && dbProduct.toLowerCase().contains("postgresql");
            } catch (Exception e) {
                isPostgres = false;
            }
        }
        return Boolean.TRUE.equals(isPostgres);
    }

    private Object toJsonbObject(String jsonString) {
        if (jsonString == null) return null;
        if (!isPostgreSql()) {
            return jsonString;
        }
        try {
            Class<?> clazz = Class.forName("org.postgresql.util.PGobject");
            Object pgo = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("setType", String.class).invoke(pgo, "jsonb");
            clazz.getMethod("setValue", String.class).invoke(pgo, jsonString);
            return pgo;
        } catch (Exception ignored) {
            return jsonString;
        }
    }

    private final RowMapper<BillingPlanRecord> planMapper = (rs, rowNum) -> new BillingPlanRecord(
            getUuid(rs, "id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("billing_period"),
            rs.getString("currency"),
            rs.getLong("price_minor"),
            rs.getBoolean("active"),
            rs.getBoolean("purchasable"),
            rs.getInt("display_order"),
            rs.getString("provider_price_id"),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at"))
    );

    private final RowMapper<PlanEntitlementRecord> entitlementMapper = (rs, rowNum) -> new PlanEntitlementRecord(
            getUuid(rs, "id"),
            getUuid(rs, "plan_id"),
            rs.getString("entitlement_key"),
            rs.getString("value_type"),
            rs.getObject("boolean_value") != null ? rs.getBoolean("boolean_value") : null,
            rs.getObject("numeric_value") != null ? rs.getLong("numeric_value") : null,
            toInstant(rs.getTimestamp("created_at"))
    );

    private final RowMapper<StudioSubscriptionRecord> subscriptionMapper = (rs, rowNum) -> new StudioSubscriptionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "plan_id"),
            SubscriptionStatus.valueOf(rs.getString("status")),
            rs.getString("provider"),
            rs.getString("provider_customer_id"),
            rs.getString("provider_subscription_id"),
            toInstant(rs.getTimestamp("current_period_start")),
            toInstant(rs.getTimestamp("current_period_end")),
            rs.getBoolean("cancel_at_period_end"),
            toInstant(rs.getTimestamp("cancelled_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getLong("version"),
            hasColumn(rs, "source_type") && rs.getString("source_type") != null ? rs.getString("source_type") : "PAID_PROVIDER",
            hasColumn(rs, "downgrade_plan_id") ? getUuid(rs, "downgrade_plan_id") : null,
            hasColumn(rs, "grace_period_end") ? toInstant(rs.getTimestamp("grace_period_end")) : null
    );

    private final RowMapper<BillingTransactionRecord> transactionMapper = (rs, rowNum) -> new BillingTransactionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "subscription_id"),
            rs.getString("provider"),
            rs.getString("provider_payment_id"),
            rs.getString("provider_order_id"),
            rs.getLong("amount_minor"),
            rs.getString("currency"),
            BillingTransactionStatus.valueOf(rs.getString("status")),
            rs.getString("description"),
            rs.getString("receipt_url"),
            toInstant(rs.getTimestamp("occurred_at")),
            toInstant(rs.getTimestamp("created_at"))
    );

    private final RowMapper<BillingCheckoutSessionRecord> checkoutSessionMapper = (rs, rowNum) -> new BillingCheckoutSessionRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "plan_id"),
            rs.getString("provider"),
            rs.getString("provider_session_id"),
            rs.getString("idempotency_key"),
            rs.getString("status"),
            rs.getLong("amount_minor"),
            rs.getString("currency"),
            rs.getString("success_url"),
            rs.getString("cancel_url"),
            toInstant(rs.getTimestamp("expires_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at"))
    );

    @Override
    public Optional<BillingPlanRecord> findPlanByCode(String code) {
        String sql = "SELECT * FROM billing_plans WHERE code = ?";
        List<BillingPlanRecord> list = jdbcTemplate.query(sql, planMapper, code);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<BillingPlanRecord> findPlanById(UUID id) {
        String sql = "SELECT * FROM billing_plans WHERE id = ?";
        List<BillingPlanRecord> list = jdbcTemplate.query(sql, planMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<BillingPlanRecord> listActivePlans() {
        String sql = "SELECT * FROM billing_plans WHERE active = true ORDER BY display_order ASC, price_minor ASC";
        return jdbcTemplate.query(sql, planMapper);
    }

    @Override
    public List<PlanEntitlementRecord> findEntitlementsByPlanId(UUID planId) {
        String sql = "SELECT * FROM plan_entitlements WHERE plan_id = ?";
        return jdbcTemplate.query(sql, entitlementMapper, planId);
    }

    @Override
    public Optional<StudioSubscriptionRecord> findActiveSubscription(UUID studioId) {
        String sql = """
            SELECT * FROM studio_subscriptions
            WHERE studio_id = ? AND status IN ('ACTIVE', 'CANCEL_AT_PERIOD_END')
            ORDER BY created_at DESC
            LIMIT 1
        """;
        List<StudioSubscriptionRecord> list = jdbcTemplate.query(sql, subscriptionMapper, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<StudioSubscriptionRecord> findLatestSubscription(UUID studioId) {
        String sql = """
            SELECT * FROM studio_subscriptions
            WHERE studio_id = ?
            ORDER BY created_at DESC
            LIMIT 1
        """;
        List<StudioSubscriptionRecord> list = jdbcTemplate.query(sql, subscriptionMapper, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<StudioSubscriptionRecord> findSubscriptionById(UUID id) {
        String sql = "SELECT * FROM studio_subscriptions WHERE id = ?";
        List<StudioSubscriptionRecord> list = jdbcTemplate.query(sql, subscriptionMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<StudioSubscriptionRecord> findSubscriptionByProviderSubscriptionId(String provider, String providerSubscriptionId) {
        String sql = "SELECT * FROM studio_subscriptions WHERE provider = ? AND provider_subscription_id = ?";
        List<StudioSubscriptionRecord> list = jdbcTemplate.query(sql, subscriptionMapper, provider, providerSubscriptionId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public StudioSubscriptionRecord saveSubscription(StudioSubscriptionRecord sub) {
        Optional<StudioSubscriptionRecord> existing = findSubscriptionById(sub.id());
        Instant now = Instant.now();

        if (existing.isPresent()) {
            StudioSubscriptionRecord current = existing.get();
            String updateSql = """
                UPDATE studio_subscriptions SET
                    plan_id = ?,
                    status = ?,
                    provider = ?,
                    provider_customer_id = ?,
                    provider_subscription_id = ?,
                    current_period_start = ?,
                    current_period_end = ?,
                    cancel_at_period_end = ?,
                    cancelled_at = ?,
                    updated_at = ?,
                    version = version + 1,
                    source_type = ?,
                    downgrade_plan_id = ?,
                    grace_period_end = ?
                WHERE id = ? AND version = ?
            """;
            int rows = jdbcTemplate.update(updateSql,
                    sub.planId(),
                    sub.status().name(),
                    sub.provider(),
                    sub.providerCustomerId(),
                    sub.providerSubscriptionId(),
                    sub.currentPeriodStart() != null ? Timestamp.from(sub.currentPeriodStart()) : null,
                    sub.currentPeriodEnd() != null ? Timestamp.from(sub.currentPeriodEnd()) : null,
                    sub.cancelAtPeriodEnd(),
                    sub.cancelledAt() != null ? Timestamp.from(sub.cancelledAt()) : null,
                    Timestamp.from(now),
                    sub.sourceType() != null ? sub.sourceType() : "PAID_PROVIDER",
                    sub.downgradePlanId(),
                    sub.gracePeriodEnd() != null ? Timestamp.from(sub.gracePeriodEnd()) : null,
                    sub.id(),
                    current.version()
            );
            if (rows == 0) {
                throw new OptimisticLockingFailureException("Concurrent modification of subscription " + sub.id());
            }
            return findSubscriptionById(sub.id()).orElse(sub);
        } else {
            String insertSql = """
                INSERT INTO studio_subscriptions (
                    id, studio_id, plan_id, status, provider,
                    provider_customer_id, provider_subscription_id,
                    current_period_start, current_period_end,
                    cancel_at_period_end, cancelled_at, created_at, updated_at, version,
                    source_type, downgrade_plan_id, grace_period_end
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
            """;
            jdbcTemplate.update(insertSql,
                    sub.id(),
                    sub.studioId(),
                    sub.planId(),
                    sub.status().name(),
                    sub.provider(),
                    sub.providerCustomerId(),
                    sub.providerSubscriptionId(),
                    sub.currentPeriodStart() != null ? Timestamp.from(sub.currentPeriodStart()) : null,
                    sub.currentPeriodEnd() != null ? Timestamp.from(sub.currentPeriodEnd()) : null,
                    sub.cancelAtPeriodEnd(),
                    sub.cancelledAt() != null ? Timestamp.from(sub.cancelledAt()) : null,
                    Timestamp.from(sub.createdAt() != null ? sub.createdAt() : now),
                    Timestamp.from(now),
                    sub.sourceType() != null ? sub.sourceType() : "PAID_PROVIDER",
                    sub.downgradePlanId(),
                    sub.gracePeriodEnd() != null ? Timestamp.from(sub.gracePeriodEnd()) : null
            );
            return findSubscriptionById(sub.id()).orElse(sub);
        }
    }

    @Override
    public BillingTransactionRecord saveTransaction(BillingTransactionRecord tx) {
        String sql = """
            INSERT INTO billing_transactions (
                id, studio_id, subscription_id, provider, provider_payment_id,
                provider_order_id, amount_minor, currency, status, description,
                receipt_url, occurred_at, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                tx.id(),
                tx.studioId(),
                tx.subscriptionId(),
                tx.provider(),
                tx.providerPaymentId(),
                tx.providerOrderId(),
                tx.amountMinor(),
                tx.currency(),
                tx.status().name(),
                tx.description(),
                tx.receiptUrl(),
                Timestamp.from(tx.occurredAt()),
                Timestamp.from(tx.createdAt())
        );
        return tx;
    }

    @Override
    public List<BillingTransactionRecord> listTransactions(UUID studioId, int limit) {
        return listTransactions(studioId, limit, 0);
    }

    @Override
    public List<BillingTransactionRecord> listTransactions(UUID studioId, int limit, int offset) {
        String sql = "SELECT * FROM billing_transactions WHERE studio_id = ? ORDER BY occurred_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, transactionMapper, studioId, limit, offset);
    }

    @Override
    public boolean recordBillingEvent(BillingEventRecord event) {
        String detailsJson = null;
        if (event.details() != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(event.details());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize billing event details: {}", e.getMessage());
            }
        }

        String sql = """
            INSERT INTO billing_events (
                id, studio_id, event_type, provider, provider_event_id,
                details, occurred_at, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try {
            jdbcTemplate.update(sql,
                    event.id(),
                    event.studioId(),
                    event.eventType(),
                    event.provider(),
                    event.providerEventId(),
                    toJsonbObject(detailsJson),
                    Timestamp.from(event.occurredAt()),
                    Timestamp.from(event.createdAt())
            );
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Billing event deduplicated for provider={}, eventId={}", event.provider(), event.providerEventId());
            return false;
        }
    }

    @Override
    public BillingCheckoutSessionRecord saveCheckoutSession(BillingCheckoutSessionRecord s) {
        String sql = """
            INSERT INTO billing_checkout_sessions (
                id, studio_id, plan_id, provider, provider_session_id,
                idempotency_key, status, amount_minor, currency,
                success_url, cancel_url, expires_at, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql,
                s.id(),
                s.studioId(),
                s.planId(),
                s.provider(),
                s.providerSessionId(),
                s.idempotencyKey(),
                s.status(),
                s.amountMinor(),
                s.currency(),
                s.successUrl(),
                s.cancelUrl(),
                Timestamp.from(s.expiresAt()),
                Timestamp.from(s.createdAt()),
                Timestamp.from(s.updatedAt())
        );
        return s;
    }

    @Override
    public Optional<BillingCheckoutSessionRecord> findCheckoutSessionById(UUID id) {
        String sql = "SELECT * FROM billing_checkout_sessions WHERE id = ?";
        List<BillingCheckoutSessionRecord> list = jdbcTemplate.query(sql, checkoutSessionMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<BillingCheckoutSessionRecord> findCheckoutSessionByIdempotencyKey(UUID studioId, String idempotencyKey) {
        String sql = "SELECT * FROM billing_checkout_sessions WHERE studio_id = ? AND idempotency_key = ?";
        List<BillingCheckoutSessionRecord> list = jdbcTemplate.query(sql, checkoutSessionMapper, studioId, idempotencyKey);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<BillingCheckoutSessionRecord> findCheckoutSessionByProviderSessionId(String provider, String providerSessionId) {
        String sql = "SELECT * FROM billing_checkout_sessions WHERE provider = ? AND provider_session_id = ?";
        List<BillingCheckoutSessionRecord> list = jdbcTemplate.query(sql, checkoutSessionMapper, provider, providerSessionId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public void updateCheckoutSessionStatus(UUID id, String status, String providerSessionId) {
        if (providerSessionId != null) {
            String sql = "UPDATE billing_checkout_sessions SET status = ?, provider_session_id = ?, updated_at = now() WHERE id = ?";
            jdbcTemplate.update(sql, status, providerSessionId, id);
        } else {
            String sql = "UPDATE billing_checkout_sessions SET status = ?, updated_at = now() WHERE id = ?";
            jdbcTemplate.update(sql, status, id);
        }
    }

    private static boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private static UUID getUuid(ResultSet rs, String col) throws SQLException {
        Object val = rs.getObject(col);
        if (val instanceof UUID u) return u;
        if (val instanceof String s) return UUID.fromString(s);
        return null;
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
