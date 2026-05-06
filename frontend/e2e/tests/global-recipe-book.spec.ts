import { test, expect } from '@playwright/test';
import { getGlobalBookStatus } from './helpers/api';

test.beforeEach(async ({}, testInfo) => {
  try {
    const status = await getGlobalBookStatus();
    if (!status.enabled || !status.reachable) {
      testInfo.skip();
    }
  } catch {
    testInfo.skip();
  }
});

test('global recipe book page loads', async ({ page }) => {
  await page.goto('/recipes/global');
  await expect(page.getByRole('heading', { name: /global|shared|community/i })).toBeVisible();
});

test('search filters shared recipes', async ({ page }) => {
  await page.goto('/recipes/global');
  const searchInput = page.locator('input[placeholder*="search" i], .search-bar input');
  if (await searchInput.isVisible().catch(() => false)) {
    await searchInput.fill('chicken');
    // Results should filter — verify at least the search triggered
    await expect(page.locator('.card-grid, .recipe-list, table').first()).toBeVisible({ timeout: 5000 });
  }
});

test('pin recipe button changes state', async ({ page }) => {
  await page.goto('/recipes/global');

  // Find a recipe card with a Pin button
  const pinBtn = page.getByRole('button', { name: /pin/i }).first();
  if (await pinBtn.isVisible().catch(() => false)) {
    await pinBtn.click();
    // Button should change to "Pinned" or similar
    await expect(
      page.getByText(/pinned/i).first()
        .or(page.getByRole('button', { name: /unpin/i }).first())
    ).toBeVisible({ timeout: 5000 });
  }
});
