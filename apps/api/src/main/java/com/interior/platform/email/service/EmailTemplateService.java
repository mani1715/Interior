package com.interior.platform.email.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailTemplateService {

    private final String baseUrl;

    public EmailTemplateService(@Value("${app.baseUrl:http://localhost:3000}") String baseUrl) {
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "http://localhost:3000";
    }

    public record RenderedEmail(String subject, String textBody, String htmlBody) {}

    public RenderedEmail renderTeamInvitation(String studioName, String role, String relativeInviteUrl) {
        String safeStudio = HtmlUtils.htmlEscape(studioName != null ? studioName : "Interior Studio");
        String safeRole = HtmlUtils.htmlEscape(role != null ? role : "Member");
        String fullUrl = baseUrl + (relativeInviteUrl.startsWith("/") ? relativeInviteUrl : "/" + relativeInviteUrl);

        String subject = "You're invited to join " + (studioName != null ? studioName : "a studio") + " on Elégance";
        String textBody = String.format(
                "You have been invited to join %s as %s on Elégance Interior Platform.\n\n" +
                "Accept your invitation here:\n%s\n\n" +
                "This invitation expires in 7 days.",
                studioName, role, fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Join %s on Elégance</h2>
              <p>You have been invited to join <strong>%s</strong> as <strong>%s</strong>.</p>
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">Accept Invitation</a>
              </div>
              <p style="font-size: 13px; color: #666;">This invitation expires in 7 days. If you did not expect this invitation, you can safely ignore this email.</p>
            </body>
            </html>
            """, safeStudio, safeStudio, safeRole, fullUrl);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderReviewInvitation(String studioName, String clientName, String relativeReviewUrl) {
        String safeStudio = HtmlUtils.htmlEscape(studioName != null ? studioName : "Interior Studio");
        String safeClient = HtmlUtils.htmlEscape(clientName != null ? clientName : "Valued Client");
        String fullUrl = baseUrl + (relativeReviewUrl.startsWith("/") ? relativeReviewUrl : "/" + relativeReviewUrl);

        String subject = "Share your experience with " + (studioName != null ? studioName : "your designer");
        String textBody = String.format(
                "Dear %s,\n\n" +
                "%s invites you to share your feedback and experience on Elégance Interior Platform.\n\n" +
                "Submit your verified review here:\n%s\n\n" +
                "This link is valid for 30 days.",
                clientName != null ? clientName : "Client", studioName, fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Your Experience with %s</h2>
              <p>Dear %s,</p>
              <p>Thank you for partnering with <strong>%s</strong>. We invite you to share your project experience and rate your satisfaction.</p>
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">Submit Verified Review</a>
              </div>
              <p style="font-size: 13px; color: #666;">This private invitation is valid for 30 days.</p>
            </body>
            </html>
            """, safeStudio, safeClient, safeStudio, fullUrl);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderNewLeadAlert(String studioName, String clientName, String category, String relativeLeadUrl) {
        String safeStudio = HtmlUtils.htmlEscape(studioName != null ? studioName : "Studio");
        String safeClient = HtmlUtils.htmlEscape(clientName != null ? clientName : "Client");
        String safeCategory = HtmlUtils.htmlEscape(category != null ? category : "Interior Project");
        String fullUrl = baseUrl + (relativeLeadUrl.startsWith("/") ? relativeLeadUrl : "/" + relativeLeadUrl);

        String subject = "New client inquiry for " + (studioName != null ? studioName : "your studio");
        String textBody = String.format(
                "You have received a new inquiry from %s for %s.\n\n" +
                "Open your CRM workspace to view and respond:\n%s",
                clientName, category, fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">New Client Inquiry</h2>
              <p><strong>%s</strong> received a new project inquiry from <strong>%s</strong> for <em>%s</em>.</p>
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">View in CRM</a>
              </div>
            </body>
            </html>
            """, safeStudio, safeClient, safeCategory, fullUrl);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderClientFeedbackAlert(String studioName, String clientName, String feedbackType, String feedbackText, String relativeUrl) {
        String safeClient = HtmlUtils.htmlEscape(clientName != null ? clientName : "Client");
        String safeType = HtmlUtils.htmlEscape(feedbackType != null ? feedbackType : "Feedback");
        String safeText = HtmlUtils.htmlEscape(feedbackText != null ? feedbackText : "");
        String fullUrl = baseUrl + (relativeUrl.startsWith("/") ? relativeUrl : "/" + relativeUrl);

        String subject = "New client feedback: " + feedbackType;
        String textBody = String.format(
                "Client %s submitted %s:\n%s\n\nView details: %s",
                clientName, feedbackType, feedbackText != null ? feedbackText : "", fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Client Feedback Received</h2>
              <p><strong>%s</strong> submitted <strong>%s</strong>.</p>
              %s
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">Review in Workspace</a>
              </div>
            </body>
            </html>
            """,
                safeClient, safeType,
                !safeText.isBlank() ? "<blockquote style=\"border-left: 3px solid #ccc; padding-left: 12px; color: #555;\">" + safeText + "</blockquote>" : "",
                fullUrl
        );

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderSubscriptionActivated(String studioName, String planName, String relativeBillingUrl) {
        String safeStudio = HtmlUtils.htmlEscape(studioName != null ? studioName : "Studio");
        String safePlan = HtmlUtils.htmlEscape(planName != null ? planName : "Commercial Plan");
        String fullUrl = baseUrl + (relativeBillingUrl.startsWith("/") ? relativeBillingUrl : "/" + relativeBillingUrl);

        String subject = "Subscription activated for " + (studioName != null ? studioName : "your studio");
        String textBody = String.format(
                "Your subscription to %s has been activated for %s.\n\n" +
                "Manage your subscription and view entitlements:\n%s",
                planName, studioName, fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Subscription Activated</h2>
              <p>Your subscription to <strong>%s</strong> is now active for <strong>%s</strong>.</p>
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">View Plan & Usage</a>
              </div>
            </body>
            </html>
            """, safePlan, safeStudio, fullUrl);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderPaymentFailed(String studioName, String planName, String graceEndDateFormatted, String relativeBillingUrl) {
        String safeStudio = HtmlUtils.htmlEscape(studioName != null ? studioName : "Studio");
        String safePlan = HtmlUtils.htmlEscape(planName != null ? planName : "Commercial Plan");
        String safeGraceDate = HtmlUtils.htmlEscape(graceEndDateFormatted != null ? graceEndDateFormatted : "the grace period deadline");
        String fullUrl = baseUrl + (relativeBillingUrl.startsWith("/") ? relativeBillingUrl : "/" + relativeBillingUrl);

        String subject = "Payment action required: Subscription payment failed for " + (studioName != null ? studioName : "your studio");
        String textBody = String.format(
                "Payment for your %s subscription (%s) could not be processed.\n\n" +
                "A grace period has begun until %s. To prevent service disruption, please update your payment method:\n%s",
                planName, studioName, graceEndDateFormatted, fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #c53030;">Payment Action Required</h2>
              <p>Payment for your <strong>%s</strong> subscription (%s) could not be processed.</p>
              <p>A grace period is active until <strong>%s</strong>. Your portfolio and projects remain fully intact.</p>
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #c53030; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">Update Payment Method</a>
              </div>
            </body>
            </html>
            """, safePlan, safeStudio, safeGraceDate, fullUrl);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderSubscriptionRenewed(String studioName, String planName, String nextPeriodFormatted, String relativeBillingUrl) {
        String safeStudio = HtmlUtils.htmlEscape(studioName != null ? studioName : "Studio");
        String safePlan = HtmlUtils.htmlEscape(planName != null ? planName : "Commercial Plan");
        String safePeriod = HtmlUtils.htmlEscape(nextPeriodFormatted != null ? nextPeriodFormatted : "");
        String fullUrl = baseUrl + (relativeBillingUrl.startsWith("/") ? relativeBillingUrl : "/" + relativeBillingUrl);

        String subject = "Subscription renewed: " + (studioName != null ? studioName : "your studio");
        String textBody = String.format(
                "Your subscription to %s for %s has been renewed successfully.\n" +
                "Next renewal: %s\n\n" +
                "View invoices and usage:\n%s",
                planName, studioName, nextPeriodFormatted, fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Subscription Renewed</h2>
              <p>Your subscription to <strong>%s</strong> for <strong>%s</strong> has renewed.</p>
              <p>Next period ends: <strong>%s</strong></p>
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">View Billing Details</a>
              </div>
            </body>
            </html>
            """, safePlan, safeStudio, safePeriod, fullUrl);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderCancelAtPeriodEnd(String studioName, String planName, String accessUntilFormatted, String relativeBillingUrl) {
        String safeStudio = HtmlUtils.htmlEscape(studioName != null ? studioName : "Studio");
        String safePlan = HtmlUtils.htmlEscape(planName != null ? planName : "Commercial Plan");
        String safeDate = HtmlUtils.htmlEscape(accessUntilFormatted != null ? accessUntilFormatted : "end of billing cycle");
        String fullUrl = baseUrl + (relativeBillingUrl.startsWith("/") ? relativeBillingUrl : "/" + relativeBillingUrl);

        String subject = "Subscription scheduled for cancellation: " + (studioName != null ? studioName : "your studio");
        String textBody = String.format(
                "Your subscription to %s for %s is scheduled to end on %s.\n\n" +
                "You have full access to your plan until this date. All project data and photos remain permanently preserved.\n\n" +
                "Manage your subscription:\n%s",
                planName, studioName, accessUntilFormatted, fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Cancellation Scheduled</h2>
              <p>Your <strong>%s</strong> subscription for <strong>%s</strong> will end on <strong>%s</strong>.</p>
              <p>You retain full premium features until that date. All your studio data remains intact.</p>
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">Manage Subscription</a>
              </div>
            </body>
            </html>
            """, safePlan, safeStudio, safeDate, fullUrl);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderSecurityAlert(String userName, String eventTitle, String eventDetails, String relativeSecurityUrl) {
        String safeUser = HtmlUtils.htmlEscape(userName != null ? userName : "User");
        String safeTitle = HtmlUtils.htmlEscape(eventTitle != null ? eventTitle : "Security Alert");
        String safeDetails = HtmlUtils.htmlEscape(eventDetails != null ? eventDetails : "");
        String fullUrl = baseUrl + (relativeSecurityUrl.startsWith("/") ? relativeSecurityUrl : "/" + relativeSecurityUrl);

        String subject = "Security notification: " + (eventTitle != null ? eventTitle : "Account update");
        String textBody = String.format(
                "Hello %s,\n\n" +
                "A security action was performed on your account: %s\n" +
                "%s\n\n" +
                "If this was not you, please review your active sessions immediately:\n%s",
                userName, eventTitle, eventDetails != null ? eventDetails : "", fullUrl
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Security Alert: %s</h2>
              <p>Hello <strong>%s</strong>,</p>
              <p>A security event occurred on your account: <em>%s</em></p>
              %s
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">Review Account Security</a>
              </div>
            </body>
            </html>
            """, safeTitle, safeUser, safeTitle,
                !safeDetails.isBlank() ? "<p style=\"color: #555;\">" + safeDetails + "</p>" : "",
                fullUrl);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderPrivacyExportReady(String userName, String relativeDownloadUrl, String expiryDays) {
        String safeUser = HtmlUtils.htmlEscape(userName != null ? userName : "User");
        String safeExpiry = HtmlUtils.htmlEscape(expiryDays != null ? expiryDays : "7 days");
        String fullUrl = baseUrl + (relativeDownloadUrl.startsWith("/") ? relativeDownloadUrl : "/" + relativeDownloadUrl);

        String subject = "Your personal data export is ready for download";
        String textBody = String.format(
                "Hello %s,\n\n" +
                "Your requested personal and studio data export archive has been generated.\n\n" +
                "Download your archive securely here:\n%s\n\n" +
                "For your security, this download link requires authentication and will expire in %s.\n" +
                "Never share this link with anyone.",
                userName != null ? userName : "User", fullUrl, safeExpiry
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Your Data Export is Ready</h2>
              <p>Hello <strong>%s</strong>,</p>
              <p>Your requested personal and studio data archive has been generated and is ready for download.</p>
              <div style="margin: 32px 0;">
                <a href="%s" style="background-color: #111; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 4px; display: inline-block;">Download Data Archive (ZIP)</a>
              </div>
              <p style="font-size: 13px; color: #666;">For your privacy, this download link requires active authentication and expires in %s. Raw data archives are never sent directly as unencrypted email attachments.</p>
            </body>
            </html>
            """, safeUser, fullUrl, safeExpiry);

        return new RenderedEmail(subject, textBody, htmlBody);
    }

    public RenderedEmail renderAccountClosureConfirmation(String userName, String effectiveDateFormatted) {
        String safeUser = HtmlUtils.htmlEscape(userName != null ? userName : "User");
        String safeDate = HtmlUtils.htmlEscape(effectiveDateFormatted != null ? effectiveDateFormatted : "today");

        String subject = "Account closure confirmation: Elégance";
        String textBody = String.format(
                "Hello %s,\n\n" +
                "This email confirms that your account on Elégance Interior Platform has been closed on %s.\n\n" +
                "Your personal profile data has been anonymized or queued for permanent deletion in accordance with our data retention policy.\n" +
                "Thank you for being part of the Elégance design community.",
                userName != null ? userName : "User", effectiveDateFormatted != null ? effectiveDateFormatted : "today"
        );

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #1a1a1a; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="font-weight: 400; font-size: 24px; color: #111;">Account Closure Confirmed</h2>
              <p>Hello <strong>%s</strong>,</p>
              <p>Your account on Elégance Interior Platform was successfully closed on <strong>%s</strong>.</p>
              <p>Your personal data and profile associations have been scheduled for permanent anonymization or erasure per platform retention schedules.</p>
              <p style="font-size: 13px; color: #666;">If this action was not initiated by you, contact platform administration immediately.</p>
            </body>
            </html>
            """, safeUser, safeDate);

        return new RenderedEmail(subject, textBody, htmlBody);
    }
}
