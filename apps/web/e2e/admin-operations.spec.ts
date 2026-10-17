import { test, expect } from '@playwright/test';

test.describe('Admin Console & Operations Certification', () => {
  test.beforeEach(async ({ page }) => {
    // Mock CSRF token
    await page.route('**/api/v1/auth/csrf', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ csrfToken: 'admin-csrf-token-123' }),
      });
    });

    // Mock Auth Me as Super Admin
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          id: 'admin-super-1',
          email: 'admin@platform.local',
          displayName: 'Platform Administrator',
          roles: ['SUPER_ADMIN', 'ADMIN'],
          activeStudioId: null,
          studios: [],
        }),
      });
    });

    // Mock Admin Operations State
    await page.route('**/api/v1/admin/operations/state**', async (route) => {
      if (route.request().method() === 'PUT') {
        const payload = route.request().postDataJSON() || {};
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'state-1',
            maintenanceMode: payload.maintenanceMode || 'NORMAL',
            maintenanceMessage: payload.maintenanceMessage || null,
            uploadsEnabled: payload.uploadsEnabled ?? true,
            aiEnabled: payload.aiEnabled ?? true,
            registrationsEnabled: payload.registrationsEnabled ?? true,
            updatedBy: 'admin@platform.local',
            updatedAt: new Date().toISOString(),
          }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'state-1',
            maintenanceMode: 'NORMAL',
            maintenanceMessage: null,
            uploadsEnabled: true,
            aiEnabled: true,
            registrationsEnabled: true,
            updatedBy: 'system',
            updatedAt: '2026-10-15T00:00:00Z',
          }),
        });
      }
    });

    // Mock Admin Operations Providers Matrix
    await page.route('**/api/v1/admin/operations/providers**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          providers: {
            STORAGE: { status: 'AVAILABLE', providerName: 'LOCAL_S3_MOCK', details: null },
            AI: { status: 'AVAILABLE', providerName: 'MOCK_ENGINE', details: null },
            EMAIL: { status: 'AVAILABLE', providerName: 'LOGGING_MOCK', details: null },
            BILLING: { status: 'AVAILABLE', providerName: 'MOCK_GATEWAY', details: null },
          },
          timestamp: '2026-10-16T12:00:00Z',
        }),
      });
    });

    // Mock Admin Operations Jobs
    await page.route('**/api/v1/admin/operations/jobs**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'job-1',
            jobKey: 'SESSION_CLEANUP',
            instanceId: 'inst-1',
            status: 'COMPLETED',
            startedAt: '2026-10-16T12:00:00Z',
            completedAt: '2026-10-16T12:00:05Z',
            processedCount: 14,
            failureCode: null,
            errorSummary: null,
          },
        ]),
      });
    });
  });

  test('should load admin operations dashboard with system state and providers', async ({ page }) => {
    await page.goto('/admin/operations');

    // Verify main admin heading
    await expect(page.locator('h1, h2').first()).toBeVisible();

    // Verify platform status and availability cards are rendered
    await expect(page.getByText('Platform Status')).toBeVisible();
    await expect(page.getByText('Availability Controls')).toBeVisible();
  });

  test('should view review moderation queue in admin console', async ({ page }) => {
    await page.route('**/api/v1/admin/reviews**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'rev-1',
            studioId: 'studio-1',
            studioName: 'Atelier Elégance',
            leadId: 'lead-1',
            rating: 5,
            title: 'Exquisite attention to detail',
            reviewText: 'Elena and her team completely transformed our flat in Worli.',
            reviewerDisplayName: 'Sunita Rao',
            displayNameMode: 'FULL_NAME',
            status: 'SUBMITTED',
            studioResponseText: null,
            studioResponseAt: null,
            submittedAt: '2026-10-14T08:00:00Z',
            publishedAt: null,
            reportCount: 0,
          },
        ]),
      });
    });

    await page.goto('/admin/reviews');
    await expect(page.getByText('Review Moderation')).toBeVisible();
    await expect(page.getByText('Sunita Rao')).toBeVisible();
  });

  test('should view audit log events in admin console', async ({ page }) => {
    await page.route('**/api/v1/admin/audit**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'audit-1',
            studioId: 'studio-1',
            actorId: 'admin-super-1',
            actorEmail: 'admin@platform.local',
            action: 'STUDIO_VERIFIED',
            resourceType: 'STUDIO',
            resourceId: 'studio-1',
            requestId: 'req-audit-123',
            details: 'Studio verification documents validated',
            timestamp: '2026-10-16T10:00:00Z',
          },
        ]),
      });
    });

    await page.goto('/admin/audit');
    await expect(page.getByText('Admin Audit Trail')).toBeVisible();
    await expect(page.getByText('STUDIO_VERIFIED')).toBeVisible();
  });
});
