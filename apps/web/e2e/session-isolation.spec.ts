import { test, expect } from '@playwright/test';

test.describe('Multi-Studio Tab & Session Isolation Certification', () => {
  test('should enforce strict tenant isolation across separate browser contexts without storage bleeding', async ({
    browser,
  }) => {
    const STUDIO_A_ID = '11111111-1111-1111-1111-111111111111';
    const STUDIO_B_ID = '22222222-2222-2222-2222-222222222222';

    let capturedHeaderA: string | null = null;
    let capturedHeaderB: string | null = null;

    // Context A (Studio A)
    const contextA = await browser.newContext();
    await contextA.addCookies([
      { name: 'session', value: 'session-cookie-a', url: 'http://localhost:3000' },
    ]);
    const pageA = await contextA.newPage();

    await pageA.route('**/api/v1/auth/csrf', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ csrfToken: 'token-a' }) });
    });

    await pageA.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          id: 'user-a',
          displayName: 'Designer Alpha',
          roles: ['DESIGNER', 'DESIGNER_ADMIN'],
          activeStudioId: STUDIO_A_ID,
          activeStudioRole: 'DESIGNER_ADMIN',
          studios: [{ studioId: STUDIO_A_ID, studioName: 'Studio Alpha', studioSlug: 'studio-alpha', role: 'DESIGNER_ADMIN' }],
        }),
      });
    });

    await pageA.route('**/api/v1/workspace/summary**', async (route) => {
      capturedHeaderA = route.request().headers()['x-studio-id'] || null;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: { displayName: 'Designer Alpha', email: 'alpha@studio.local', roles: ['DESIGNER'] },
          studio: { id: STUDIO_A_ID, name: 'Studio Alpha', slug: 'studio-alpha' },
          completeness: { profileCompletenessPercentage: 100, platformReadinessPercentage: 100, status: 'READY' },
          checklist: [],
          moduleReadiness: [],
          allStudios: [],
        }),
      });
    });

    // Context B (Studio B)
    const contextB = await browser.newContext();
    await contextB.addCookies([
      { name: 'session', value: 'session-cookie-b', url: 'http://localhost:3000' },
    ]);
    const pageB = await contextB.newPage();

    await pageB.route('**/api/v1/auth/csrf', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ csrfToken: 'token-b' }) });
    });

    await pageB.route('**/api/v1/auth/me', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          id: 'user-b',
          displayName: 'Designer Beta',
          roles: ['DESIGNER', 'DESIGNER_ADMIN'],
          activeStudioId: STUDIO_B_ID,
          activeStudioRole: 'DESIGNER_ADMIN',
          studios: [{ studioId: STUDIO_B_ID, studioName: 'Studio Beta', studioSlug: 'studio-beta', role: 'DESIGNER_ADMIN' }],
        }),
      });
    });

    await pageB.route('**/api/v1/workspace/summary**', async (route) => {
      capturedHeaderB = route.request().headers()['x-studio-id'] || null;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: { displayName: 'Designer Beta', email: 'beta@studio.local', roles: ['DESIGNER'] },
          studio: { id: STUDIO_B_ID, name: 'Studio Beta', slug: 'studio-beta' },
          completeness: { profileCompletenessPercentage: 100, platformReadinessPercentage: 100, status: 'READY' },
          checklist: [],
          moduleReadiness: [],
          allStudios: [],
        }),
      });
    });

    // Seed session storage in Context A
    await pageA.addInitScript((studioId) => {
      window.sessionStorage.setItem('interior_active_studio_id', studioId);
    }, STUDIO_A_ID);

    // Seed session storage in Context B
    await pageB.addInitScript((studioId) => {
      window.sessionStorage.setItem('interior_active_studio_id', studioId);
    }, STUDIO_B_ID);

    // Navigate Page A to workspace
    await pageA.goto('/workspace');
    await pageA.waitForSelector('main', { timeout: 15000 });

    // Navigate Page B to workspace
    await pageB.goto('/workspace');
    await pageB.waitForSelector('main', { timeout: 15000 });

    // Assert that headers strictly reflect isolated studios
    expect(capturedHeaderA).toBe(STUDIO_A_ID);
    expect(capturedHeaderB).toBe(STUDIO_B_ID);
    expect(capturedHeaderA).not.toBe(capturedHeaderB);

    // Clean up
    await contextA.close();
    await contextB.close();
  });
});
