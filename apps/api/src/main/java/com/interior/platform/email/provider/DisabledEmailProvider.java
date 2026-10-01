package com.interior.platform.email.provider;

import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(value = EmailProvider.class, ignored = DisabledEmailProvider.class)
public class DisabledEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(DisabledEmailProvider.class);

    @Override
    public String getProviderName() {
        return "DISABLED";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public EmailSendResult sendEmail(EmailMessage message) {
        log.info("Email requested for recipient (hash={}) with subject '{}', but transactional email provider is DISABLED/NOT_CONFIGURED.",
                message.recipientEmail() != null ? Integer.toHexString(message.recipientEmail().hashCode()) : "null",
                message.subject());
        return EmailSendResult.notConfigured("Transactional email provider is not configured. Email was not dispatched.");
    }
}
