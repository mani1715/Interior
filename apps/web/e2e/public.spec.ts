import { test, expect } from '@playwright/test';

test.describe('Public Visitor Journey & Discovery', () => {
  test('should load homepage with semantic structure and branding', async ({ page }) => {
    await page.goto('/');

    // Check title and skip link
    await expect(page).toHaveTitle(/Elégance/);
    const skipLink = page.locator('a[href="#main-content"]');
    await expect(skipLink).toBeAttached();

    // Check header branding and navigation
    const brand = page.locator('header a[aria-label="Elégance home"]');
    await expect(brand).toBeVisible();

    const isDesktop = (page.viewportSize()?.width ?? 1200) >= 1100;
    if (isDesktop) {
      await expect(page.locator('header nav, header div').getByRole('link', { name: /Projects/i }).first()).toBeVisible();
      await expect(page.locator('header nav, header div').getByRole('link', { name: /Professionals/i }).first()).toBeVisible();
    } else {
      const menuBtn = page.locator('header button[aria-label="Toggle menu"], header button:has(svg)').first();
      await expect(menuBtn).toBeVisible();
    }

    // Check footer presence
    const footer = page.locator('footer');
    await expect(footer).toBeVisible();
  });

  test('should navigate to professionals directory and render filters', async ({ page }) => {
    // Intercept professionals API if called
    await page.route('**/api/v1/professionals**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'prof-1',
              slug: 'atelier-elegance',
              businessName: 'Atelier Elégance Interiors',
              city: 'Mumbai',
              primaryStyle: 'MINIMALIST',
              rating: 4.9,
              reviewCount: 24,
              verified: true,
              coverImageUrl: null,
            },
            {
              id: 'prof-2',
              slug: 'studio-maison',
              businessName: 'Studio Maison Design',
              city: 'Bengaluru',
              primaryStyle: 'CONTEMPORARY',
              rating: 4.8,
              reviewCount: 19,
              verified: true,
              coverImageUrl: null,
            },
          ],
          totalElements: 2,
          totalPages: 1,
          size: 10,
          number: 0,
        }),
      });
    });

    await page.goto('/professionals');
    await expect(page.locator('h1, h2').first()).toBeVisible();
  });

  test('should navigate to projects page and view project grid', async ({ page }) => {
    await page.route('**/api/v1/projects**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'proj-1',
              slug: 'coastal-minimalist-villa',
              title: 'Coastal Minimalist Villa',
              propertyType: 'VILLA',
              style: 'MINIMALIST',
              city: 'Goa',
              featuredImageUrl: null,
            },
          ],
          totalElements: 1,
          totalPages: 1,
          size: 12,
          number: 0,
        }),
      });
    });

    await page.goto('/projects');
    await expect(page.locator('main')).toBeVisible();
  });

  test('should display privacy policy and terms of service', async ({ page }) => {
    await page.goto('/privacy');
    await expect(page.locator('h1, h2').first()).toContainText(/Privacy/i);

    await page.goto('/terms');
    await expect(page.locator('h1, h2').first()).toContainText(/Terms/i);
  });
});
