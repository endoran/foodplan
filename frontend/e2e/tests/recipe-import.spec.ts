import { test, expect } from '@playwright/test';

test.describe('Recipe Import', () => {
  test('import page loads with URL input', async ({ page }) => {
    await page.goto('/recipes/import');
    await expect(page.locator('input[type="url"]')).toBeVisible();
  });
});
