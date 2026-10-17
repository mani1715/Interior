import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 60000,
  expect: {
    timeout: 10000,
  },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3099',
    trace: 'off',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'mobile-smoke',
      use: { ...devices['Pixel 5'] },
      testMatch: /public\.spec\.ts/,
    },
  ],
  webServer: {
    command: 'npx next start -p 3099',
    url: 'http://localhost:3099',
    reuseExistingServer: false,
    timeout: 60000,
  },
});
