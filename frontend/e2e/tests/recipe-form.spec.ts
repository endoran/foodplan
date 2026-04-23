import { test, expect } from '@playwright/test';

test.describe('Recipe Form \u2014 fraction input (#260)', () => {
  test('accepts simple fraction in quantity field', async ({ page }) => {
    await page.goto('/recipes/new');
    await page.getByRole('button', { name: /add ingredient/i }).click();
    const qtyInput = page.locator('input[placeholder*="1/2"]').first();
    await qtyInput.fill('1/2');
    await expect(qtyInput).toHaveValue('1/2');
  });

  test('accepts mixed fraction in quantity field', async ({ page }) => {
    await page.goto('/recipes/new');
    await page.getByRole('button', { name: /add ingredient/i }).click();
    const qtyInput = page.locator('input[placeholder*="1/2"]').first();
    await qtyInput.fill('1 1/2');
    await expect(qtyInput).toHaveValue('1 1/2');
  });

  test('accepts decimal in quantity field', async ({ page }) => {
    await page.goto('/recipes/new');
    await page.getByRole('button', { name: /add ingredient/i }).click();
    const qtyInput = page.locator('input[placeholder*="1/2"]').first();
    await qtyInput.fill('2.5');
    await expect(qtyInput).toHaveValue('2.5');
  });
});

test.describe('Recipe Form \u2014 servings clearing (#264)', () => {
  // When #264 is fixed, REMOVE test.fail or this test will start failing
  test.fail('servings field can be cleared and retyped \u2014 KNOWN BUG #264', async ({ page }) => {
    await page.goto('/recipes/new');
    const servingsInput = page.locator('input[type="number"]').first();
    await expect(servingsInput).toBeVisible();
    await servingsInput.fill('');
    await servingsInput.pressSequentially('4');
    await expect(servingsInput).toHaveValue('4');
  });
});
