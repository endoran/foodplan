import { test, expect } from '@playwright/test';

test.describe('Recipe Scan', () => {
  test('scan page loads with upload form', async ({ page }) => {
    await page.goto('/recipes/scan');
    await expect(page.locator('input[type="file"]')).toBeVisible();
  });

  test('scan button enables after file selection', async ({ page }) => {
    await page.goto('/recipes/scan');
    const fileInput = page.locator('input[type="file"]');
    const buffer = Buffer.from('fake-image-data');
    await fileInput.setInputFiles({
      name: 'test-recipe.jpg',
      mimeType: 'image/jpeg',
      buffer,
    });
    await expect(page.getByRole('button', { name: /scan/i })).toBeEnabled();
  });
});
