import { test, expect } from '@playwright/test';

test.describe('Quick Cook', () => {
  test('quick cook page loads', async ({ page }) => {
    await page.goto('/quick-cook');
    await expect(page.locator('body')).not.toBeEmpty();
  });

  test('quantity field accepts fractions', async ({ page }) => {
    await page.goto('/quick-cook');
    const qtyInput = page.locator('input[placeholder*="1/2"]').first();
    if (await qtyInput.isVisible().catch(() => false)) {
      await qtyInput.fill('1/4');
      await expect(qtyInput).toHaveValue('1/4');
    }
  });
});
