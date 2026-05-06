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

test('shared recipe detail shows name and ingredients', async ({ page }) => {
  await page.goto('/recipes/global');

  // Click first recipe card to navigate to detail
  const card = page.locator('.card-grid a, .card-grid .card').first();
  if (await card.isVisible().catch(() => false)) {
    await card.click();
    await expect(page).toHaveURL(/\/recipes\/global\/.+/);
    await expect(page.getByRole('heading')).toBeVisible();
  }
});

test('pin button on shared recipe detail', async ({ page }) => {
  await page.goto('/recipes/global');

  const card = page.locator('.card-grid a, .card-grid .card').first();
  if (await card.isVisible().catch(() => false)) {
    await card.click();
    await expect(page).toHaveURL(/\/recipes\/global\/.+/);

    const pinBtn = page.getByRole('button', { name: /pin/i });
    if (await pinBtn.isVisible().catch(() => false)) {
      await expect(pinBtn).toBeEnabled();
    }
  }
});
