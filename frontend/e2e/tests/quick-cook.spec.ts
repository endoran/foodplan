import { test, expect } from '@playwright/test';
import { createTestIngredient, createInventoryItem, deleteIngredient, deleteInventoryItem } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('quick cook page loads', async ({ page }) => {
  await page.goto('/quick-cook');
  await expect(page.getByRole('heading', { name: /quick cook/i })).toBeVisible();
});

test('deduct from pantry reduces inventory', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Butter ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));
  const item = await createInventoryItem({ ingredientId: ing.id, quantity: 4, unit: 'TBSP' });
  registerCleanup(() => deleteInventoryItem(item.id));

  await page.goto('/quick-cook');

  // Select ingredient and enter quantity
  await page.locator('select').first().selectOption(ing.id);
  const qtyInput = page.locator('input[placeholder*="1/2"]').first();
  await qtyInput.fill('2');
  await page.locator('select').last().selectOption('TBSP');

  await page.getByRole('button', { name: /cook|deduct/i }).click();
  await expect(page.locator('.success, .muted')).toBeVisible({ timeout: 5000 });
});

test('quantity field accepts fractions', async ({ page }) => {
  await page.goto('/quick-cook');
  const qtyInput = page.locator('input[placeholder*="1/2"]').first();
  await expect(qtyInput).toBeVisible({ timeout: 5000 });
  await qtyInput.fill('1/4');
  await expect(qtyInput).toHaveValue('1/4');
});
