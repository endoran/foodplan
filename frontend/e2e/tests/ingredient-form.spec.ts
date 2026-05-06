import { test, expect } from '@playwright/test';
import { createTestIngredient, deleteIngredient } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('create ingredient end-to-end', async ({ page }) => {
  const suffix = uniqueSuffix();
  const name = `[E2E] Paprika ${suffix}`;

  await page.goto('/ingredients/new');
  await expect(page.getByRole('heading', { name: 'New Ingredient' })).toBeVisible();

  await page.locator('input[type="text"]').fill(name);
  await page.locator('select').first().selectOption('SPICE_RACK');
  await page.locator('select').nth(1).selectOption('SPICES');
  await page.getByLabel(/gluten free/i).check();

  await page.getByRole('button', { name: /create ingredient/i }).click();

  await expect(page).toHaveURL('/ingredients', { timeout: 10_000 });
  await expect(page.getByText(name)).toBeVisible();

  // Cleanup: find and delete via API
  const row = page.getByText(name).locator('..').locator('..');
  const editLink = row.getByRole('link', { name: 'Edit' });
  const href = await editLink.getAttribute('href');
  const id = href?.split('/').at(-2) || '';
  if (id) registerCleanup(() => deleteIngredient(id));
});

test('edit ingredient changes category', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Butter ${suffix}`, storageCategory: 'REFRIGERATED', groceryCategory: 'DAIRY' });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto(`/ingredients/${ing.id}/edit`);
  await expect(page.getByRole('heading', { name: 'Edit Ingredient' })).toBeVisible();

  await page.locator('select').nth(1).selectOption('BAKING');
  await page.getByRole('button', { name: /save changes/i }).click();

  await expect(page).toHaveURL('/ingredients', { timeout: 10_000 });

  // Verify the change
  await page.goto(`/ingredients/${ing.id}/edit`);
  await expect(page.locator('select').nth(1)).toHaveValue('BAKING');
});

test('shopping list exclude toggle persists', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Water ${suffix}`, shoppingListExclude: false });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto(`/ingredients/${ing.id}/edit`);
  const checkbox = page.getByLabel(/exclude from shopping list/i);
  await expect(checkbox).not.toBeChecked();
  await checkbox.check();
  await page.getByRole('button', { name: /save changes/i }).click();
  await expect(page).toHaveURL('/ingredients', { timeout: 10_000 });

  await page.goto(`/ingredients/${ing.id}/edit`);
  await expect(page.getByLabel(/exclude from shopping list/i)).toBeChecked();
});

test('dietary tags persist after save', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Almond Milk ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto(`/ingredients/${ing.id}/edit`);
  await page.getByLabel(/dairy free/i).check();
  await page.getByLabel(/vegan/i).check();
  await page.getByRole('button', { name: /save changes/i }).click();
  await expect(page).toHaveURL('/ingredients', { timeout: 10_000 });

  await page.goto(`/ingredients/${ing.id}/edit`);
  await expect(page.getByLabel(/dairy free/i)).toBeChecked();
  await expect(page.getByLabel(/vegan/i)).toBeChecked();
});
