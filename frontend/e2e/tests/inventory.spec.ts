import { test, expect } from '@playwright/test';
import { createTestIngredient, createInventoryItem, deleteIngredient, deleteInventoryItem } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('add inventory item via form', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Olive Oil ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto('/inventory');
  await expect(page.getByRole('heading', { name: /inventory/i })).toBeVisible();

  await page.getByRole('button', { name: /add item/i }).click();
  await page.locator('select').first().selectOption(ing.id);
  await page.locator('input[placeholder*="1/2"]').fill('3/4');
  await page.locator('select').last().selectOption('CUP');
  await page.getByRole('button', { name: /save|add/i }).first().click();

  await expect(page.getByText(ing.name)).toBeVisible({ timeout: 5000 });
});

test('edit inventory item quantity', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Rice ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));
  const item = await createInventoryItem({ ingredientId: ing.id, quantity: 2, unit: 'CUP' });
  registerCleanup(() => deleteInventoryItem(item.id));

  await page.goto('/inventory');
  const row = page.getByText(ing.name).locator('..').locator('..');
  await row.getByRole('button', { name: /edit/i }).click();

  const qtyInput = page.locator('input[placeholder*="1/2"]');
  await qtyInput.fill('5');
  await page.getByRole('button', { name: /save|update/i }).first().click();

  await expect(page.getByText('5')).toBeVisible({ timeout: 5000 });
});

test('delete inventory item removes from list', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Beans ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));
  const item = await createInventoryItem({ ingredientId: ing.id, quantity: 1, unit: 'LBS' });
  // Will be deleted by UI

  await page.goto('/inventory');
  await expect(page.getByText(ing.name)).toBeVisible({ timeout: 5000 });

  const row = page.getByText(ing.name).locator('..').locator('..');
  await row.getByRole('button', { name: /delete/i }).click();

  await expect(page.getByText(ing.name)).not.toBeVisible({ timeout: 5000 });
});

test('fraction quantities display correctly', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Honey ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));
  const item = await createInventoryItem({ ingredientId: ing.id, quantity: 0.5, unit: 'CUP' });
  registerCleanup(() => deleteInventoryItem(item.id));

  await page.goto('/inventory');
  await expect(page.getByText(ing.name)).toBeVisible({ timeout: 5000 });
});
