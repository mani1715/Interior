# Frontend Container Specification (Next.js 16 Standalone)

## 1. Container Overview

- **Source Dockerfile:** [`apps/web/Dockerfile`](file:///c:/my%20projects/interior%20design/apps/web/Dockerfile)
- **Base Image:** `node:20-alpine` (Node.js 20 LTS Alpine)
- **Framework Version:** Next.js 16.3.8 (Turbopack)
- **Output Mode:** `standalone` (server.js + bundled dependencies)
- **Container Architecture:** Multi-stage build (Deps -> Builder -> Runner)
- **Image Size:** ~299 MB total uncompressed (~79 MB compressed content)
- **Default Port:** `3000` (HTTP)

---

## 2. Security & Filesystem Contract

### 2.1. Non-Root Execution
The container drops root privileges and executes as an unprivileged system user:
```dockerfile
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs
USER nextjs
```
Runtime verification confirms:
```bash
$ docker run --rm --entrypoint whoami interior-platform-web:local
nextjs
```

### 2.2. Isolated Standalone Artifacts
Only the standalone output, compiled static chunks, and public assets are copied to the runtime image:
```dockerfile
COPY --from=builder /app/apps/web/public ./apps/web/public
COPY --from=builder --chown=nextjs:nodejs /app/apps/web/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/apps/web/.next/static ./apps/web/.next/static
```
- Development tools, test harnesses (Vitest, Playwright), and temporary build caches are strictly omitted from the runner stage.
- Zero secrets or `.env` files are included.

### 2.3. Source Map Policy
- **Policy:** Client-side production browser source maps are **DISABLED** by default (`productionBrowserSourceMaps: false`).
- Only minified production JavaScript bundles are served to users. No proprietary source code or development paths are exposed to browsers.

---

## 3. Runtime Healthcheck Contract

- **Health Route:** `/health`
- **Response Format:** Static JSON / HTML returning HTTP 200 with status `UP` and timestamp.
- **Container Probe:**
```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:3000/health || exit 1
```

---

## 4. Hosting Compatibility

- **Docker / OCI Compatible:** Native container execution via `node apps/web/server.js`.
- **Vercel / Managed Edge Compatible:** The project retains standard Next.js directory structure and environment conventions, allowing deployment on managed platforms without vendor lock-in.
