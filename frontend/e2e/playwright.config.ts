import { defineConfig, devices } from '@playwright/test';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: 'list',
  timeout: 30_000,
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:9090',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /global-setup\.ts/,
    },
    {
      name: 'desktop-chrome',
      use: {
        ...devices['Desktop Chrome'],
        storageState: join(__dirname, '.auth', 'user.json'),
      },
      dependencies: ['setup'],
    },
    {
      name: 'iphone-safari',
      use: {
        ...devices['iPhone 15'],
        storageState: join(__dirname, '.auth', 'user.json'),
      },
      dependencies: ['setup'],
    },
  ],
  outputDir: join(__dirname, 'test-results'),
});
