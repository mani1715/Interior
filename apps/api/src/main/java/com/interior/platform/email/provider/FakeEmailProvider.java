package com.interior.platform.email.provider;

import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic test & simulation email provider for test harness and development verification.
 * Simulates success, transient failure, permanent failure, bounces, and complaints.
 * Never connects to any third-party network or live API.
 */
public class FakeEmailProvider implements EmailProvider {

    public enum SimulationMode {
        SUCCESS,
        TRANSIENT_FAILURE,
        PERMANENT_FAILURE,
        THROW_EXCEPTION
    }

    private volatile SimulationMode mode = SimulationMode.SUCCESS;
    private final List<EmailMessage> sentMessages = new CopyOnWriteArrayList<>();
    private final Map<String, EmailMessage> messagesById = new ConcurrentHashMap<>();

    @Override
    public String getProviderName() {
        return "FAKE_SIMULATED";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    public void setSimulationMode(SimulationMode mode) {
        this.mode = mode;
    }

    public List<EmailMessage> getSentMessages() {
        return sentMessages;
    }

    public Map<String, EmailMessage> getMessagesById() {
        return messagesById;
    }

    public void clear() {
        sentMessages.clear();
        messagesById.clear();
        this.mode = SimulationMode.SUCCESS;
    }

    @Override
    public EmailSendResult sendEmail(EmailMessage message) {
        switch (mode) {
            case TRANSIENT_FAILURE -> {
                return EmailSendResult.transientFailure("RATE_LIMITED", "Simulated transient upstream rate limit");
            }
            case PERMANENT_FAILURE -> {
                return EmailSendResult.permanentFailure("RECIPIENT_REJECTED", "Simulated mailbox does not exist (permanent failure)");
            }
            case THROW_EXCEPTION -> {
                throw new RuntimeException("Simulated upstream network timeout or connection reset");
            }
            case SUCCESS -> {
                String msgId = "sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                sentMessages.add(message);
                messagesById.put(msgId, message);
                return EmailSendResult.success(msgId);
            }
            default -> {
                return EmailSendResult.success("sim_default");
            }
        }
    }
}
