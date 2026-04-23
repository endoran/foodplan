import { test as setup, expect } from '@playwright/test';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const authFile = join(__dirname, '..', '.auth', 'user.json');

setup('authenticate', async ({ page }) => {
  const email = process.env.TEST_EMAIL;
  const password = process.env.TEST_PASSWORD;

  if (!email || !password) {
    throw new Error('TEST_EMAIL and TEST_PASSWORD env vars required. Set them in e2e/.env.local');
  }

  page.on('response', resp => {
    if (resp.url().includes('/auth/login')) {
      console.log('Login response:', resp.status(), resp.url());
    }
  });

  await page.goto('/login');
  await page.locator('input[type="email"]').fill(email);
  await page.locator('input[type="password"]').fill(password);

  const [response] = await Promise.all([
    page.waitForResponse(resp => resp.url().includes('/auth/login')),
    page.getByRole('button', { name: /sign in/i }).click(),
  ]);

  console.log('Login API status:', response.status());
  if (response.status() !== 200) {
    const body = await response.text();
    console.log('Login API body:', body);
  }

  await expect(page).toHaveURL(/\/recipes/, { timeout: 10_000 });
  await page.context().storageState({ path: authFile });
});
