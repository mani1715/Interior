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
}
