import { test, expect } from '@playwright/test';

test('import page loads with URL input and button', async ({ page }) => {
  await page.goto('/recipes/import');
  await expect(page.getByRole('heading', { name: /import recipe/i })).toBeVisible();
  await expect(page.locator('input[type="url"]')).toBeVisible();
  await expect(page.getByRole('button', { name: /import/i })).toBeVisible();
});

test('url param auto-triggers import attempt', async ({ page }) => {
  await page.goto('/recipes/import?url=https://example.com/nonexistent-recipe');

  // Should show loading state or error (URL won't be reachable)
  await expect(
    page.getByText(/importing/i)
      .or(page.locator('.error'))
      .or(page.getByText(/import failed/i))
  ).toBeVisible({ timeout: 15_000 });
});

test('manual URL submission shows loading then result or error', async ({ page }) => {
  await page.goto('/recipes/import');
  await page.locator('input[type="url"]').fill('https://example.com/fake-recipe');
  await page.getByRole('button', { name: /import/i }).click();

  // Should transition to loading, then error (not reachable from staging)
  await expect(
    page.getByText(/importing/i)
      .or(page.locator('.error'))
  ).toBeVisible({ timeout: 15_000 });
});

test('discard resets the import form', async ({ page }) => {
  await page.goto('/recipes/import?url=https://example.com/test');

  // Wait for import attempt to finish (error or preview)
  await expect(
    page.locator('.error')
      .or(page.locator('.recipe-form'))
  ).toBeVisible({ timeout: 15_000 });

  // If preview loaded, discard it
  const discardBtn = page.getByRole('button', { name: /discard/i });
  if (await discardBtn.isVisible().catch(() => false)) {
    await discardBtn.click();
    await expect(page.locator('.recipe-form')).not.toBeVisible();
    await expect(page.locator('input[type="url"]')).toHaveValue('');
  }
});
