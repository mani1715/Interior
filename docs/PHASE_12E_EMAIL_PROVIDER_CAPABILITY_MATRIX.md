# Phase 12E: Transactional Email Provider Capability Matrix

> **Comparative Evaluation:** Candidate Providers for Elégance Interior Designer Platform  
> **Evaluated Candidates:** Resend (Primary Candidate) vs AWS SES (Secondary Candidate)  
> **Status:** Evaluated & Implemented behind `EmailProvider` | Live Provider Activation Deferred to Phase 15

---

## 1. Capability & Feature Comparison Matrix

| Evaluation Dimension | Resend (Primary Candidate) | AWS Simple Email Service (SES v2) | Architectural Impact / Recommendation |
| :--- | :--- | :--- | :--- |
| **Primary Protocol** | Modern REST API over HTTP/2 | AWS SDK v2 (`software.amazon.awssdk:sesv2`) / HTTPS | Resend requires zero external AWS SDK dependencies; lightweight native Java 21 `HttpClient`. SES utilizes official AWS SDK. |
| **Authentication Method** | Simple Bearer API Key (`re_...`) | IAM Access Keys / IAM Instance Role / OIDC Assumed Role | Resend is simpler for containerized deployment; AWS SES is ideal if infrastructure runs entirely on AWS (ECS/EKS/EC2). |
| **Sender Domain Verification**| Automated DNS wizard (DKIM, SPF, DMARC, MX) with real-time verification | Route 53 or manual DNS CNAME/TXT records for DKIM, MAIL FROM MX/SPF, and DMARC | Both require standard DNS records. Resend provides faster real-time verification feedback in developer UI. |
| **Sandbox Environment** | Clean sandbox mode: immediate sending to account owner email; verified domains send unrestricted | Sandbox restriction by default (sending only to verified recipient emails) until production access request approved | Resend allows immediate testing of verified domains without manual justification tickets. SES requires submitting a quota increase ticket. |
| **Webhook Infrastructure** | Native HTTPS webhooks signed with Svix / HMAC-SHA256 headers (`svix-id`, `svix-timestamp`, `svix-signature`) | Amazon SNS HTTP/HTTPS subscription requiring SNS message confirmation & topic verification | Resend delivers direct webhook payloads. SES requires SNS topic wiring and initial subscription handshake. |
| **Bounce & Complaint Handling**| Direct webhook events (`email.bounced`, `email.complained`) with reason codes | SNS notifications forwarded from SES event destination to platform webhook | Platform `EmailWebhookController` supports both Resend native format and SES SNS JSON format. |
| **Rate Limiting & Retries** | 10–100 req/sec depending on tier; returns HTTP `429 Too Many Requests` | Starts at 14 emails/sec (sandbox), scales to hundreds; returns `LimitExceededException` | Both adapters translate rate limits into retryable transient failures. |
| **Pricing / Cost Profile** | Free tier: 3,000 emails/mo, 100/day. Pro: $20/mo for 50,000 emails. | Pay-as-you-go: $0.10 per 1,000 emails ($0.00 for first 62,000 if sent from EC2). | SES is cheaper at massive scale; Resend is significantly simpler to configure, maintain, and debug during pre-launch and early growth. |
| **Developer Experience** | Industry-leading dashboard, live delivery logs, HTML email preview, instant DNS checks | AWS Management Console, CloudWatch metrics, SNS topic configuration | Resend is vastly superior for rapid debugging and team visibility. |

---

## 2. Selection Recommendation

- **Recommended Primary for Phase 15 Activation:** **Resend**
  - **Rationale:** Minimal operational friction, clean developer dashboard, native HTTP/2 REST API with Java 21 `HttpClient`, and instant domain verification. For an interior designer platform with strictly transactional volume (~1,000–10,000 emails/month at launch), Resend delivers the optimal developer ergonomics and reliability.
- **Recommended Secondary / Fallback:** **AWS SES**
  - **Rationale:** SES is fully implemented and contract-tested. If corporate hosting policy mandates unified AWS infrastructure or cost optimization at ultra-high volumes (>100k emails/month), SES can be activated simply by setting `EMAIL_PROVIDER=ses` with zero code changes.

---

## 3. Configuration Comparison

### Resend Configuration
```properties
app.email.provider=resend
app.email.from-address=notifications@elegance.interior
app.email.from-display-name=Elégance Platform
app.email.reply-to=support@elegance.interior
app.email.resend.api-key=${RESEND_API_KEY}
app.email.webhook-secret=${EMAIL_WEBHOOK_SECRET}
```

### AWS SES Configuration
```properties
app.email.provider=ses
app.email.from-address=notifications@elegance.interior
app.email.from-display-name=Elégance Platform
app.email.reply-to=support@elegance.interior
app.email.ses.region=us-east-1
# AWS credentials provided via IAM instance role or environment:
# AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
# AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
```
