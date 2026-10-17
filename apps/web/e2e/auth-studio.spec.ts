import { test, expect } from '@playwright/test';

test.describe('Designer Workspace & Studio Journey', () => {
  test.beforeEach(async ({ page, context }) => {
    // Set session cookie so Next.js server component boundary in WorkspaceLayout validates authentication
    await context.addCookies([
      {
        name: 'session',
        value: 'dev-mock-session-cookie',
        url: 'http://localhost:3000',
      },
    ]);

    // Mock CSRF token
    await page.route('**/api/v1/auth/csrf', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ csrfToken: 'mock-csrf-token-xyz' }),
      });
    });

    // Mock Auth Me endpoint with authenticated: true
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          id: 'user-designer-1',
          email: 'dev.designer@platform.local',
          displayName: 'Elena Rostova',
          roles: ['DESIGNER', 'DESIGNER_ADMIN'],
          activeStudioId: 'studio-1',
          activeStudioRole: 'DESIGNER_ADMIN',
          studios: [
            {
              studioId: 'studio-1',
              studioName: 'Atelier Elégance',
              studioSlug: 'atelier-elegance',
              role: 'DESIGNER_ADMIN',
            },
          ],
        }),
      });
    });

    // Mock Providers endpoint for Sign-In
    await page.route('**/api/v1/auth/providers', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          providers: [],
          devAuthEnabled: true,
          devPersonas: [
            {
              key: 'designer_owner',
              displayName: 'Elena Rostova (Designer Owner)',
              email: 'dev.designer@platform.local',
              role: 'DESIGNER_ADMIN',
              assurance: 'HIGH',
            },
          ],
        }),
      });
    });

    // Mock Dev Login
    await page.route('**/api/v1/auth/dev-login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: {
          'Set-Cookie': 'session=mock-valid-session; Path=/; SameSite=Lax',
        },
        body: JSON.stringify({
          authenticated: true,
          id: 'user-designer-1',
          email: 'dev.designer@platform.local',
          displayName: 'Elena Rostova',
          roles: ['DESIGNER', 'DESIGNER_ADMIN'],
          activeStudioId: 'studio-1',
          activeStudioRole: 'DESIGNER_ADMIN',
          studios: [
            {
              studioId: 'studio-1',
              studioName: 'Atelier Elégance',
              studioSlug: 'atelier-elegance',
              role: 'DESIGNER_ADMIN',
            },
          ],
        }),
      });
    });

    // Mock Workspace Summary
    await page.route('**/api/v1/workspace/summary**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: {
            displayName: 'Elena Rostova',
            email: 'dev.designer@platform.local',
            roles: ['DESIGNER', 'DESIGNER_ADMIN'],
          },
          studio: {
            id: 'studio-1',
            name: 'Atelier Elégance',
            slug: 'atelier-elegance',
            professionalType: 'INTERIOR_DESIGNER',
            professionalTitle: 'Principal Architect',
            tagline: 'Timeless luxury and residential design',
            role: 'DESIGNER_ADMIN',
            operationalStatus: 'ACTIVE',
            publicationStatus: 'PUBLISHED',
            city: 'Mumbai',
            state: 'Maharashtra',
            serviceCount: 4,
            specialtyCount: 3,
            serviceAreaCount: 2,
            contactCount: 1,
          },
          completeness: {
            profileCompletenessPercentage: 85,
            platformReadinessPercentage: 90,
            status: 'READY',
            breakdown: {
              identityScore: 100,
              locationScore: 100,
              servicesScore: 80,
              specialtiesScore: 80,
              contactsScore: 100,
              portfolioScore: 90,
              projectsScore: 85,
            },
          },
          checklist: [],
          moduleReadiness: [],
          allStudios: [],
        }),
      });
    });

    // Mock Studio Billing Summary
    await page.route('**/api/v1/studio/billing**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          studioId: 'studio-1',
          currentPlan: {
            id: 'plan-pro',
            code: 'STUDIO_PRO',
            name: 'Studio Pro',
            currency: 'INR',
            priceMinor: 499900,
            active: true,
            purchasable: true,
            displayOrder: 2,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
          effectiveEntitlements: {
            PROJECT_LIMIT: 50,
            CINEMATIC_PROJECT_LIMIT: 20,
            STORAGE_LIMIT_BYTES: 10737418240,
            AI_MONTHLY_CREDIT_LIMIT: 100,
          },
          billingProviderStatus: 'LOCAL_MOCK',
          commercialCheckoutEnabled: true,
          availablePlans: [],
          recentTransactions: [],
          usage: {
            projectCount: 1,
            projectLimit: 50,
            cinematicProjectCount: 0,
            cinematicProjectLimit: 20,
            cinematicPortfolioAllowed: true,
            storageBytesUsed: 524288000,
            storageLimitBytes: 10737418240,
            aiCreditsUsedThisMonth: 12,
            aiMonthlyCreditLimit: 100,
          },
        }),
      });
    });
  });

  test('should sign in via dev persona and load workspace dashboard', async ({ page, context }) => {
    // Start unauthenticated so sign-in view is displayed
    await context.clearCookies();
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ authenticated: false }),
      });
    });

    await page.goto('/sign-in');

    // Wait for dev persona button
    const personaBtn = page.getByRole('button', { name: /Elena Rostova/i });
    await expect(personaBtn).toBeVisible({ timeout: 10000 });

    // When logging in, mock auth/me to authenticated
    await page.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          id: 'user-designer-1',
          displayName: 'Elena Rostova',
          roles: ['DESIGNER', 'DESIGNER_ADMIN'],
          activeStudioId: 'studio-1',
          activeStudioRole: 'DESIGNER_ADMIN',
          studios: [{ studioId: 'studio-1', studioName: 'Atelier Elégance', studioSlug: 'atelier-elegance', role: 'DESIGNER_ADMIN' }],
        }),
      });
    });
    await context.addCookies([
      { name: 'session', value: 'dev-valid-session', url: 'http://localhost:3000' },
    ]);

    await personaBtn.click();

    // Verify redirection to workspace
    await page.waitForURL('**/workspace**', { timeout: 15000 });
    await expect(page.locator('main')).toBeVisible();
    await expect(page.getByText(/Atelier Elégance|Workspace/i).first()).toBeVisible();
  });

  test('should navigate to projects list, search, and view projects', async ({ page }) => {
    await page.route('**/api/v1/projects**', async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'proj-new-1',
            studioId: 'studio-1',
            slug: 'modern-minimalist-penthouse',
            title: 'Modern Minimalist Penthouse',
            categoryCode: 'COMPLETE_HOME_INTERIOR',
            categoryDisplayName: 'Complete Home Interior',
            projectStatus: 'READY',
            visibilityStatus: 'PORTFOLIO',
            featured: false,
            displayOrder: 1,
            styleCodes: ['MODERN_MINIMALIST'],
            styleDisplayNames: ['Modern Minimalist'],
            country: 'IN',
            city: 'Mumbai',
            state: 'Maharashtra',
            rooms: [],
            mediaAssets: [],
            version: 1,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          }),
        });
      } else {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'proj-1',
              slug: 'penthouse-boulevard',
              title: 'Penthouse Boulevard',
              categoryCode: 'COMPLETE_HOME_INTERIOR',
              categoryDisplayName: 'Complete Home Interior',
              projectStatus: 'READY',
              visibilityStatus: 'PORTFOLIO',
              featured: true,
              displayOrder: 1,
              city: 'Mumbai',
              state: 'Maharashtra',
              propertyType: 'APARTMENT',
              projectScope: 'FULL_INTERIOR',
              styleCodes: ['MODERN_MINIMALIST'],
              styleDisplayNames: ['Modern Minimalist'],
              completionYear: 2026,
              version: 1,
              createdAt: '2026-01-15T10:00:00Z',
              updatedAt: '2026-03-20T12:00:00Z',
            },
          ]),
        });
      }
    });

    await page.goto('/workspace/projects');
    await expect(page.getByText('Penthouse Boulevard')).toBeVisible();

    // Verify search input operates
    const searchInput = page.getByPlaceholder(/Search/i);
    if (await searchInput.isVisible()) {
      await searchInput.fill('Penthouse');
      await expect(page.getByText('Penthouse Boulevard')).toBeVisible();
    }
  });

  test('should view leads list in workspace', async ({ page }) => {
    await page.route('**/api/v1/workspace/leads**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'lead-1',
              clientName: 'Aarav Sharma',
              clientEmail: 'aarav@domain.local',
              projectScope: 'Full 3BHK Renovation',
              status: 'NEW',
              budget: '₹25,00,000',
              createdAt: '2026-10-10T14:30:00Z',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          size: 10,
          number: 0,
        }),
      });
    });

    await page.goto('/workspace/leads');
    await expect(page.locator('main')).toBeVisible();
  });

  test('should view billing entitlements in workspace', async ({ page }) => {
    await page.goto('/workspace/billing');
    await expect(page.locator('h1, h2').first()).toBeVisible();
  });
});
