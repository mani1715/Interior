package com.interior.platform.email.provider;

import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;

public interface EmailProvider {
    String getProviderName();
    boolean isConfigured();
    EmailSendResult sendEmail(EmailMessage message);
}
