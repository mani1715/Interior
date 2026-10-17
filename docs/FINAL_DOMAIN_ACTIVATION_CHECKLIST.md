# Final Production Domain Activation Checklist
## Elégance — Interior Designer Platform

This checklist records all configuration updates required once the final production domain name is decided by the platform owner in **Phase 16**.

> [!NOTE]
> **Status: DEFERRED TO PHASE 16.**
> Do not execute these steps until the custom domain has been registered and commercial brand identity is locked.

---

### 1. Cloudflare DNS & TLS Configuration
- [ ] Add domain to Cloudflare account (e.g. `example-interior.com`).
- [ ] Configure `A` / `CNAME` records pointing to production hosting cluster (frontend and API).
- [ ] Set SSL/TLS Encryption Mode to **Full (Strict)**.
- [ ] Enable **Always Use HTTPS** and **Automatic HTTPS Rewrites**.
- [ ] Configure minimum TLS version: `TLS 1.3` (fallback `TLS 1.2`).

### 2. Cross-Origin Resource Sharing (CORS) & Allowed Origins
- [ ] In backend environment, update `APP_SECURITY_ALLOWED_ORIGINS`:
  - From: `http://localhost:3000`
  - To: `https://example-interior.com,https://www.example-interior.com,https://api.example-interior.com`
- [ ] Remove wildcard or localhost origins from production profile.

### 3. Google Identity Services (OIDC) Redirect URIs
- [ ] In Google Cloud Console > APIs & Services > Credentials:
  - Add Authorized JavaScript Origins: `https://example-interior.com`
  - Add Authorized Redirect URI: `https://api.example-interior.com/api/v1/login/oauth2/code/google`
- [ ] Update `GOOGLE_REDIRECT_URI` environment variable.

### 4. Cloudflare R2 CORS & Public Media Hostname
- [ ] In Cloudflare R2 > `interior-platform-media-private` > CORS Policy:
  - Update `AllowedOrigins` to include `https://example-interior.com`.
- [ ] In Cloudflare R2 > `interior-platform-media-public`:
  - Connect Custom Domain: `media.example-interior.com`.
- [ ] Update platform environment variable `PUBLIC_MEDIA_BASE_URL`:
  - Set to: `https://media.example-interior.com`.

### 5. Email Domain Authentication (DNS Records)
- [ ] In DNS provider, configure TXT and CNAME records supplied by Resend / AWS SES:
  - **SPF**: `v=spf1 include:amazonses.com ~all` (or Resend include)
  - **DKIM**: 3x CNAME records for cryptographic email signing
  - **DMARC**: `v=DMARC1; p=reject; rua=mailto:dmarc@example-interior.com`
- [ ] Verify domain status reports `VERIFIED` in email provider console.
- [ ] Update `EMAIL_FROM_ADDRESS` to `notifications@example-interior.com`.

### 6. Payment Gateway Return & Webhook URLs
- [ ] In Stripe / Razorpay Dashboard:
  - Update Webhook Endpoint URL: `https://api.example-interior.com/api/v1/billing/webhook/stripe`
  - Update Brand settings, Statement Descriptor, and Support Website.
- [ ] In backend environment, update `app.billing.allowed-redirect-hosts` to include `example-interior.com`.

### 7. Content Security Policy (CSP) & Session Cookies
- [ ] Update `Content-Security-Policy` header in Next.js `next.config.js` and Spring Security to permit media from `media.example-interior.com`.
- [ ] Ensure session cookie `__Host-session` is served strictly over HTTPS with `SameSite=Lax`, `Secure`, and `HttpOnly`.

### 8. SEO Canonical URLs & Sitemap
- [ ] Update `NEXT_PUBLIC_APP_URL` to `https://example-interior.com`.
- [ ] Verify `sitemap.xml` dynamically generates all URLs using the canonical production domain.
- [ ] Verify `robots.txt` points to `https://example-interior.com/sitemap.xml`.
