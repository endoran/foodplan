import { test, expect } from '@playwright/test';

test('settings page loads with current values', async ({ page }) => {
  await page.goto('/settings');
  await expect(page.getByRole('heading', { name: /settings/i })).toBeVisible();
  // Should show timezone, default servings, and allowed sites sections
  await expect(page.getByText(/timezone|time zone/i)).toBeVisible();
  await expect(page.getByText(/servings/i)).toBeVisible();
});

test('update default servings and save', async ({ page }) => {
  await page.goto('/settings');

  const servingsInput = page.locator('input[type="number"]').first();
  const originalValue = await servingsInput.inputValue();
  const newValue = originalValue === '4' ? '6' : '4';

  await servingsInput.fill(newValue);
  await page.getByRole('button', { name: /save/i }).click();

  // Verify save confirmation
  await expect(page.getByText(/saved|success/i)).toBeVisible({ timeout: 5000 });

  // Reload and verify persisted
  await page.reload();
  await expect(servingsInput).toHaveValue(newValue, { timeout: 5000 });

  // Restore original value
  await servingsInput.fill(originalValue);
  await page.getByRole('button', { name: /save/i }).click();
  await expect(page.getByText(/saved|success/i)).toBeVisible({ timeout: 5000 });
});

test('add and remove allowed recipe site', async ({ page }) => {
  await page.goto('/settings');

  // Add a test site
  const siteInput = page.locator('.add-site-row input');
  if (await siteInput.isVisible().catch(() => false)) {
    await siteInput.fill('e2e-test-site.example.com');
    await page.locator('.add-site-row').getByRole('button', { name: /add/i }).click();

    // Verify it appears in the list
    await expect(page.getByText('e2e-test-site.example.com')).toBeVisible();

    // Save
    await page.getByRole('button', { name: /save/i }).click();
    await expect(page.getByText(/saved|success/i)).toBeVisible({ timeout: 5000 });

    // Remove it
    const siteRow = page.getByText('e2e-test-site.example.com').locator('..');
    await siteRow.getByRole('button').click();
    await page.getByRole('button', { name: /save/i }).click();
    await expect(page.getByText(/saved|success/i)).toBeVisible({ timeout: 5000 });
  }
});
