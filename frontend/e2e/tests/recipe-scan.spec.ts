import { test, expect } from '@playwright/test';

test('scan page loads with file upload', async ({ page }) => {
  await page.goto('/recipes/scan');
  await expect(page.getByRole('heading', { name: /scan/i })).toBeVisible();
  await expect(page.locator('input[type="file"]')).toBeVisible();
});

test('file selection enables scan button', async ({ page }) => {
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

test('scan button triggers API call and shows loading or error', async ({ page }) => {
  await page.goto('/recipes/scan');
  const fileInput = page.locator('input[type="file"]');

  // Create a minimal valid JPEG (just enough bytes to pass frontend validation)
  const buffer = Buffer.from('fake-image-data-for-testing-purposes');
  await fileInput.setInputFiles({
    name: 'recipe-card.jpg',
    mimeType: 'image/jpeg',
    buffer,
  });

  await page.getByRole('button', { name: /scan/i }).click();

  // Should show loading state or error (OCR may fail with fake image)
  await expect(
    page.getByText(/scanning|processing/i)
      .or(page.locator('.error'))
      .or(page.locator('.recipe-form'))
  ).toBeVisible({ timeout: 30_000 });
});
