package com.interior.platform.email.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 12E — Email Security, HTML Escaping, Header Injection & Multilingual Tests")
class EmailSecurityAndEscapingTest {

    private EmailTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new EmailTemplateService("https://elegance.design/");
    }

    @Test
    @DisplayName("HTML Escaping: Script tags and event handlers are neutralized in team invite")
    void testHtmlEscapingTeamInvite() {
        String xssStudio = "<script>alert('xss')</script> Studio & Co \"Premium\"";
        String xssRole = "<img src=x onerror=alert(1)> Senior Designer";

        EmailTemplateService.RenderedEmail email = templateService.renderTeamInvitation(
                xssStudio,
                xssRole,
                "/invite/accept?token=tok_123"
        );

        assertFalse(email.htmlBody().contains("<script>"));
        assertFalse(email.htmlBody().contains("<img src=x"));
        assertTrue(email.htmlBody().contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt; Studio &amp; Co &quot;Premium&quot;"));
        assertTrue(email.htmlBody().contains("&lt;img src=x onerror=alert(1)&gt; Senior Designer"));
        assertTrue(email.htmlBody().contains("https://elegance.design/invite/accept?token=tok_123"));
    }

    @Test
    @DisplayName("HTML Escaping: Quotes, ampersands, and angle brackets neutralized in review invite")
    void testHtmlEscapingReviewInvite() {
        String studioName = "Studio & Design 'A-List' <p>test</p>";
        String clientName = "Maneesh <script>document.cookie</script>";

        EmailTemplateService.RenderedEmail email = templateService.renderReviewInvitation(
                studioName,
                clientName,
                "/review/submit?token=rev_456"
        );

        assertFalse(email.htmlBody().contains("<script>"));
        assertFalse(email.htmlBody().contains("<p>test</p>"));
        assertTrue(email.htmlBody().contains("Maneesh &lt;script&gt;document.cookie&lt;/script&gt;"));
        assertTrue(email.htmlBody().contains("https://elegance.design/review/submit?token=rev_456"));
    }

    @Test
    @DisplayName("HTML Escaping: Privacy export and account closure sanitize user names")
    void testHtmlEscapingPrivacyTemplates() {
        String maliciousName = "<b>Admin</b> \"User\" & 'Tester'";

        EmailTemplateService.RenderedEmail exportEmail = templateService.renderPrivacyExportReady(
                maliciousName,
                "/account/privacy/download?id=exp_123",
                "7 days"
        );
        assertFalse(exportEmail.htmlBody().contains("<b>Admin</b>"));
        assertTrue(exportEmail.htmlBody().contains("&lt;b&gt;Admin&lt;/b&gt; &quot;User&quot; &amp; &#39;Tester&#39;"));

        EmailTemplateService.RenderedEmail closureEmail = templateService.renderAccountClosureConfirmation(
                maliciousName,
                "October 16, 2026"
        );
        assertFalse(closureEmail.htmlBody().contains("<b>Admin</b>"));
        assertTrue(closureEmail.htmlBody().contains("&lt;b&gt;Admin&lt;/b&gt; &quot;User&quot; &amp; &#39;Tester&#39;"));
    }

    @Test
    @DisplayName("CRLF Injection: Carriage return and newline characters are rejected in recipient and subject")
    void testCrlfHeaderInjectionDefense() {
        assertThrows(IllegalArgumentException.class, () ->
                CommunicationDeliveryService.validateHeaderSafety("designer@example.com\r\nBcc: evil@example.com", "Safe Subject")
        );
        assertThrows(IllegalArgumentException.class, () ->
                CommunicationDeliveryService.validateHeaderSafety("designer@example.com\nBcc: evil@example.com", "Safe Subject")
        );
        assertThrows(IllegalArgumentException.class, () ->
                CommunicationDeliveryService.validateHeaderSafety("designer@example.com", "Subject\r\nCc: spy@example.com")
        );
        assertThrows(IllegalArgumentException.class, () ->
                CommunicationDeliveryService.validateHeaderSafety("designer@example.com", "Subject\nInjected: value")
        );

        assertDoesNotThrow(() ->
                CommunicationDeliveryService.validateHeaderSafety("designer+valid.name@example.com", "Safe Subject with Numbers 123!")
        );
    }

    @Test
    @DisplayName("Multilingual Text: English, Telugu, Hindi, and Tamil content preserve UTF-8 integrity in HTML and plain text")
    void testMultilingualIntegrity() {
        String teluguStudio = "రాయల్ ఇంటీరియర్స్ హైదరాబాద్ (Royal Interiors)";
        String hindiStudio = "शानदार इंटीरियर डिज़ाइन (Splendid Design)";
        String tamilStudio = "அழகான உள்துறை வடிவமைப்பு (Elegant Design)";

        EmailTemplateService.RenderedEmail teluguEmail = templateService.renderTeamInvitation(teluguStudio, "Lead Architect", "/invite/1");
        assertTrue(teluguEmail.htmlBody().contains("రాయల్ ఇంటీరియర్స్ హైదరాబాద్ (Royal Interiors)"));
        assertTrue(teluguEmail.textBody().contains("రాయల్ ఇంటీరియర్స్ హైదరాబాద్ (Royal Interiors)"));

        EmailTemplateService.RenderedEmail hindiEmail = templateService.renderReviewInvitation(hindiStudio, "अमित शर्मा", "/review/1");
        assertTrue(hindiEmail.htmlBody().contains("शानदार इंटीरियर डिज़ाइन (Splendid Design)"));
        assertTrue(hindiEmail.htmlBody().contains("अमित शर्मा"));

        EmailTemplateService.RenderedEmail tamilEmail = templateService.renderNewLeadAlert(tamilStudio, "சுரேஷ் குமார்", "வாழும் அறை", "/leads/1");
        assertTrue(tamilEmail.htmlBody().contains("அழகான உள்துறை வடிவமைப்பு (Elegant Design)"));
        assertTrue(tamilEmail.htmlBody().contains("சுரேஷ் குமார்"));
    }

    @Test
    @DisplayName("Data Bounds: Very long names and descriptions (250+ characters) do not throw exceptions")
    void testLongDataHandling() {
        String longStudio = "A".repeat(300) + " Studio";
        String longClient = "B".repeat(300) + " Client";
        String longFeedback = "C".repeat(1000);

        EmailTemplateService.RenderedEmail email = templateService.renderClientFeedbackAlert(
                longStudio,
                longClient,
                "Detailed Material Revision",
                longFeedback,
                "/workspace/feedback/123"
        );

        assertNotNull(email.subject());
        assertNotNull(email.htmlBody());
        assertNotNull(email.textBody());
        assertTrue(email.htmlBody().contains("B".repeat(300)));
        assertTrue(email.htmlBody().contains("C".repeat(1000)));
    }

    @Test
    @DisplayName("Base URL: Trailing slashes are normalized and relative URLs are safely joined")
    void testBaseUrlNormalization() {
        EmailTemplateService trailingSlashService = new EmailTemplateService("https://elegance.design///");
        EmailTemplateService.RenderedEmail email1 = trailingSlashService.renderTeamInvitation("Studio", "Role", "invite/token1");
        assertTrue(email1.htmlBody().contains("href=\"https://elegance.design/invite/token1\""));

        EmailTemplateService.RenderedEmail email2 = trailingSlashService.renderTeamInvitation("Studio", "Role", "/invite/token2");
        assertTrue(email2.htmlBody().contains("href=\"https://elegance.design/invite/token2\""));
    }
}
