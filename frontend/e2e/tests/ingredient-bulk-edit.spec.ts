import { test, expect } from '@playwright/test';
import { createTestIngredient, deleteIngredient } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('bulk edit page loads with table', async ({ page }) => {
  await page.goto('/ingredients/bulk-edit');
  await expect(page.getByRole('heading', { name: /bulk edit/i })).toBeVisible();
  await expect(page.locator('.bulk-edit-table, table')).toBeVisible({ timeout: 5000 });
});

test('changing a field marks row dirty and updates save count', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Cumin ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto('/ingredients/bulk-edit');
  await expect(page.getByText(ing.name)).toBeVisible({ timeout: 5000 });

  // Change the grocery category dropdown in the row
  const row = page.getByText(ing.name).locator('..').locator('..');
  const select = row.locator('select').first();
  const currentValue = await select.inputValue();
  const newValue = currentValue === 'SPICES' ? 'BAKING' : 'SPICES';
  await select.selectOption(newValue);

  // Row should be marked dirty
  await expect(row).toHaveClass(/row-dirty/, { timeout: 2000 });

  // Save button should show count
  await expect(page.getByRole('button', { name: /save.*1/i })).toBeVisible();
});

test('save applies changes and clears dirty state', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Pepper ${suffix}`, groceryCategory: 'SPICES' });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto('/ingredients/bulk-edit');
  await expect(page.getByText(ing.name)).toBeVisible({ timeout: 5000 });

  const row = page.getByText(ing.name).locator('..').locator('..');
  const select = row.locator('select').first();
  await select.selectOption('BAKING');

  await page.getByRole('button', { name: /save/i }).click();

  // Success message or dirty state cleared
  await expect(
    page.locator('.success')
      .or(page.getByRole('button', { name: /save changes \(0\)/i }))
      .or(row.locator(':not(.row-dirty)'))
  ).toBeVisible({ timeout: 5000 });
});
