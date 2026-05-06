import { test, expect } from '@playwright/test';

test.use({ storageState: { cookies: [], origins: [] } });

test('login page displays form fields', async ({ page }) => {
  await page.goto('/login');
  await expect(page.locator('input[type="email"]')).toBeVisible();
  await expect(page.locator('input[type="password"]')).toBeVisible();
  await expect(page.getByRole('button', { name: /sign in/i })).toBeVisible();
});

test('invalid credentials show error message', async ({ page }) => {
  await page.goto('/login');
  await page.locator('input[type="email"]').fill('fake@example.com');
  await page.locator('input[type="password"]').fill('wrongpassword');
  await page.getByRole('button', { name: /sign in/i }).click();

  await expect(page.locator('.error')).toBeVisible({ timeout: 5000 });
});

test('link to register page', async ({ page }) => {
  await page.goto('/login');
  await page.getByRole('link', { name: /register|sign up|create account/i }).click();
  await expect(page).toHaveURL(/\/register/);
});

test('unauthenticated user redirected to login', async ({ page }) => {
  await page.goto('/recipes');
  await expect(page).toHaveURL(/\/login/, { timeout: 5000 });
});
