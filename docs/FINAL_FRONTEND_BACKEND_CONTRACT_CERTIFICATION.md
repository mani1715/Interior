# Final Frontend / Backend Contract Certification
## Elégance — Interior Designer Platform

This document certifies the contract alignment between the Next.js TypeScript frontend API client and the Spring Boot Java REST controllers.

---

## 1. REST API Contract Alignment Matrix

| Frontend Client Method | HTTP | Target Endpoint URL | Spring Boot Controller Method | DTO & Payload Alignment | Status |
| :--- | :---: | :--- | :--- | :--- | :--- |
| `authApi.getCsrfToken()` | `GET` | `/api/v1/auth/csrf` | `AuthController.getCsrfToken()` | `{ csrfToken: string }` | PASS |
| `authApi.initiateLogin()` | `GET` | `/api/v1/auth/login?provider=...`| `AuthController.login()` | OIDC 302 Redirect (PKCE + Nonce) | PASS |
| `authApi.devLogin()` | `POST` | `/api/v1/auth/dev-login` (Dev/Test) | `AuthController.devLogin()` | Persona -> Session Cookie | PASS |
| `authApi.logout()` | `POST` | `/api/v1/auth/logout` | `AuthController.logout()` | Void -> Revoke Session | PASS |
| `mfaApi.enrollTotp()` | `POST` | `/api/v1/auth/mfa/enroll` | `MfaController.enrollTotp()` | `{ secret, qrUri, recoveryCodes }`| PASS |
| `mfaApi.verifyTotp()` | `POST` | `/api/v1/auth/mfa/verify` | `MfaController.verifyTotp()` | `{ code }` -> `{ verified: true }` | PASS |
| `discoveryApi.getProjects()`| `GET` | `/api/v1/projects` | `ProjectController.listPublic()` | `Page<PublicProjectSummary>` | PASS |
| `discoveryApi.getProject()` | `GET` | `/api/v1/projects/{slug}` | `ProjectController.getBySlug()` | `PublicProjectDetailResponse` | PASS |
| `discoveryApi.getProfessionals()`|`GET`| `/api/v1/professionals` | `StudioController.listPublic()` | `Page<PublicStudioSummary>` | PASS |
| `discoveryApi.getProfessional()`|`GET`| `/api/v1/professionals/{slug}`|`StudioController.getBySlug()` | `PublicStudioDetailResponse` | PASS |
| `workspaceApi.getWorkspace()`| `GET` | `/api/v1/studios/{id}/workspace`|`WorkspaceController.getWorkspace()`| `StudioWorkspaceResponse` | PASS |
| `workspaceApi.createStudio()` | `POST` | `/api/v1/studios` | `StudioController.createStudio()` | `CreateStudioRequest` (UUIDv7) | PASS |
| `projectApi.createProject()` | `POST` | `/api/v1/projects` | `ProjectController.createProject()` | `CreateProjectRequest` | PASS |
| `projectApi.updateProject()` | `PUT` | `/api/v1/projects/{id}` | `ProjectController.updateProject()` | `UpdateProjectRequest` | PASS |
| `projectApi.deleteProject()` | `DELETE`| `/api/v1/projects/{id}` | `ProjectController.deleteProject()` | Void (Soft Delete) | PASS |
| `projectApi.addRoom()` | `POST` | `/api/v1/projects/{id}/rooms` | `RoomController.createRoom()` | `CreateRoomRequest` | PASS |
| `mediaApi.createUploadIntent()`|`POST`| `/api/v1/media/upload-intent` | `MediaController.createIntent()` | `UploadIntentRequest` -> Presigned URL | PASS |
| `mediaApi.confirmUpload()` | `POST` | `/api/v1/media/upload-intent/{id}/confirm`|`MediaController.confirm()`| `{ mediaId, status: READY }` | PASS |
| `mediaApi.deleteMedia()` | `DELETE`| `/api/v1/media/{id}` | `MediaController.deleteMedia()` | Void (Pruned from R2 & DB) | PASS |
| `portfolioApi.updatePortfolio()`|`PUT` | `/api/v1/studios/{id}/portfolio`|`PortfolioController.update()` | `PortfolioUpdateRequest` | PASS |
| `portfolioApi.publishPortfolio()`|`PUT`| `/api/v1/studios/{id}/portfolio/publish`|`PortfolioController.publish()`| `PortfolioPublishResponse` | PASS |
| `leadsApi.submitInquiry()` | `POST` | `/api/v1/leads` | `LeadController.submitPublicInquiry()` | `PublicInquiryRequest` -> LeadRef | PASS |
| `leadsApi.getStudioLeads()` | `GET` | `/api/v1/leads` | `LeadController.listStudioLeads()` | `Page<StudioLeadSummary>` | PASS |
| `leadsApi.updateLeadStatus()`| `PUT` | `/api/v1/leads/{id}/status` | `LeadController.updateStatus()` | `UpdateLeadStatusRequest` | PASS |
| `reviewsApi.createInvite()` | `POST` | `/api/v1/reviews/invitations`| `ReviewController.createInvite()` | `CreateReviewInviteRequest` | PASS |
| `reviewsApi.submitReview()` | `POST` | `/api/v1/reviews` | `ReviewController.submitReview()` | `SubmitReviewRequest` | PASS |
| `aiApi.generateConcept()` | `POST` | `/api/v1/ai/generate` | `AiVisualizerController.generate()` | `AiGenerateRequest` -> JobId | PASS |
| `teamApi.inviteMember()` | `POST` | `/api/v1/team/invitations` | `TeamController.createInvite()` | `TeamInviteRequest` | PASS |
| `teamApi.acceptInvite()` | `POST` | `/api/v1/team/invitations/accept`|`TeamController.acceptInvite()` | `{ token }` -> Studio Joined | PASS |
| `billingApi.getEntitlements()`| `GET` | `/api/v1/billing/entitlements`| `BillingController.getEntitlements()`| `StudioEntitlementsResponse` | PASS |
| `adminApi.getOperationsState()`|`GET` | `/api/v1/admin/operations/state`|`AdminOpsController.getState()` | `PlatformOperationalStateDto` | PASS |
| `adminApi.updateOperationsState()`|`PUT`| `/api/v1/admin/operations/state`|`AdminOpsController.updateState()`| `UpdateOperationalStateDto` | PASS |
| `adminApi.getAuditLogs()` | `GET` | `/api/v1/admin/audit` | `AdminAuditController.listLogs()` | `Page<SecurityAuditLogDto>` | PASS |
| `adminApi.approveReview()` | `POST` | `/api/v1/admin/reviews/{id}/approve`|`AdminReviewController.approve()`| Void | PASS |
| `privacyApi.requestExport()` | `POST` | `/api/v1/privacy/export` | `PrivacyController.requestExport()` | `{ exportRequestId }` | PASS |
| `privacyApi.requestDeletion()`| `POST` | `/api/v1/privacy/delete` | `PrivacyController.requestDeletion()`| `{ deletionRequestId }` | PASS |

---

## 2. Parity & Enum Verification
- **Roles**: All 8 user roles (`PUBLIC`, `CUSTOMER`, `DESIGNER`, `DESIGNER_ADMIN`, `DESIGNER_MEMBER`, `MODERATOR`, `ADMIN`, `SUPER_ADMIN`) map identically across frontend TypeScript types and Spring Security `GrantedAuthority` records.
- **Subscription Tiers**: `STANDARD`, `PREMIUM`, `PRO` are consistent across backend quota meters and frontend UI capacity displays.
- **Media States**: `INITIALIZED`, `UPLOADED`, `SCANNING`, `PROCESSING`, `READY`, `FAILED`, `INFECTED` align 1:1 between backend entity enums and frontend status pills.
- **Operational Modes**: `NORMAL`, `READ_ONLY` are handled synchronously by both backend interceptors and frontend banner banners.
