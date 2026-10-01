package com.interior.platform.email.service;

import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import com.interior.platform.email.provider.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionalEmailService {

    private static final Logger log = LoggerFactory.getLogger(TransactionalEmailService.class);

    private final EmailProvider emailProvider;

    public TransactionalEmailService(EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    public boolean isConfigured() {
        return emailProvider.isConfigured();
    }

    public String getProviderName() {
        return emailProvider.getProviderName();
    }

    public EmailSendResult send(EmailMessage message) {
        if (message == null || message.recipientEmail() == null || message.recipientEmail().isBlank()) {
            return EmailSendResult.failure("Recipient email cannot be empty");
        }
        return emailProvider.sendEmail(message);
    }
}
