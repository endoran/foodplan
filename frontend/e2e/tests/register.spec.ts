import { test, expect } from '@playwright/test';

test.use({ storageState: { cookies: [], origins: [] } });

test('register page displays form fields', async ({ page }) => {
  await page.goto('/register');
  await expect(page.locator('input[type="email"]')).toBeVisible();
  await expect(page.locator('input[type="password"]')).toBeVisible();
  await expect(page.getByRole('button', { name: /register|sign up|create/i })).toBeVisible();
});

test('duplicate email shows error', async ({ page }) => {
  const testEmail = process.env.TEST_EMAIL || 'test@example.com';

  await page.goto('/register');
  // Fill org name if present
  const orgInput = page.locator('input[placeholder*="org" i], input[name*="org" i]');
  if (await orgInput.isVisible().catch(() => false)) {
    await orgInput.fill('E2E Duplicate Test Org');
  }
  await page.locator('input[type="email"]').fill(testEmail);
  await page.locator('input[type="password"]').fill('TestPassword123!');
  await page.getByRole('button', { name: /register|sign up|create/i }).click();

  await expect(page.locator('.error')).toBeVisible({ timeout: 5000 });
});

test('link to login page', async ({ page }) => {
  await page.goto('/register');
  await page.getByRole('link', { name: /sign in|log in|already have/i }).click();
  await expect(page).toHaveURL(/\/login/);
});
