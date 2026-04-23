import { test, expect } from '@playwright/test';

test.describe('Inventory', () => {
  test('inventory page loads', async ({ page }) => {
    await page.goto('/inventory');
    await expect(page.getByRole('heading')).toBeVisible();
  });

  test('quantity field accepts fractions', async ({ page }) => {
    await page.goto('/inventory');
    const addBtn = page.getByRole('button', { name: /add item/i });
    if (await addBtn.isVisible().catch(() => false)) {
      await addBtn.click();
    }
    const qtyInput = page.locator('input[placeholder*="1/2"]').first();
    await expect(qtyInput).toBeVisible({ timeout: 5_000 });
    await qtyInput.fill('3/4');
    await expect(qtyInput).toHaveValue('3/4');
  });
});
