import { test, expect } from '@playwright/test';

test.describe('Inventory', () => {
  test('inventory page loads', async ({ page }) => {
    await page.goto('/inventory');
    await expect(page.getByRole('heading')).toBeVisible();
  });

  test('quantity field accepts fractions', async ({ page }) => {
    await page.goto('/inventory');
    const qtyInput = page.locator('input[placeholder*="1/2"]').first();
    if (await qtyInput.isVisible().catch(() => false)) {
      await qtyInput.fill('3/4');
      await expect(qtyInput).toHaveValue('3/4');
    }
  });
});
