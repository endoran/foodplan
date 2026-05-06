import { test, expect } from '@playwright/test';
import { createTestIngredient, createTestRecipe, deleteIngredient, deleteRecipe } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('servings scaler changes ingredient quantities', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Rice ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));

  const recipe = await createTestRecipe({
    name: `[E2E] Rice Bowl ${suffix}`,
    baseServings: 2,
    ingredients: [{ ingredientId: ing.id, ingredientName: ing.name, quantity: 1, unit: 'CUP' }],
  });
  registerCleanup(() => deleteRecipe(recipe.id));

  await page.goto(`/recipes/${recipe.id}`);
  await expect(page.getByRole('heading', { name: recipe.name })).toBeVisible();

  // Base: 2 servings, 1 cup
  await expect(page.locator('.serving-count')).toHaveText('2');
  const qtyCell = page.locator('.ingredients-table tbody td:nth-child(2)').first();
  await expect(qtyCell).toHaveText('1');

  // Increment to 4 servings → quantity should double to 2
  await page.locator('.serving-scaler button:has-text("+")').click();
  await page.locator('.serving-scaler button:has-text("+")').click();
  await expect(page.locator('.serving-count')).toHaveText('4');
  await expect(qtyCell).toHaveText('2', { timeout: 5000 });
});

test('delete recipe redirects to recipe list', async ({ page }) => {
  const suffix = uniqueSuffix();
  const recipe = await createTestRecipe({ name: `[E2E] Delete Me ${suffix}` });
  // No cleanup needed — test deletes it

  await page.goto(`/recipes/${recipe.id}`);
  await expect(page.getByRole('heading', { name: recipe.name })).toBeVisible();

  page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: /delete/i }).click();

  await expect(page).toHaveURL('/recipes', { timeout: 10_000 });
  await expect(page.getByText(recipe.name)).not.toBeVisible();
});

test('edit link navigates to edit page', async ({ page }) => {
  const suffix = uniqueSuffix();
  const recipe = await createTestRecipe({ name: `[E2E] Edit Link ${suffix}` });
  registerCleanup(() => deleteRecipe(recipe.id));

  await page.goto(`/recipes/${recipe.id}`);
  await page.getByRole('link', { name: /edit/i }).click();

  await expect(page).toHaveURL(`/recipes/${recipe.id}/edit`);
  await expect(page.getByRole('heading', { name: 'Edit Recipe' })).toBeVisible();
});

test('back link returns to recipe list', async ({ page }) => {
  const suffix = uniqueSuffix();
  const recipe = await createTestRecipe({ name: `[E2E] Back Link ${suffix}` });
  registerCleanup(() => deleteRecipe(recipe.id));

  await page.goto(`/recipes/${recipe.id}`);
  await page.getByRole('link', { name: /back/i }).click();

  await expect(page).toHaveURL('/recipes', { timeout: 10_000 });
});

test('recipe with no ingredients shows empty message', async ({ page }) => {
  const suffix = uniqueSuffix();
  const recipe = await createTestRecipe({ name: `[E2E] Empty ${suffix}`, ingredients: [] });
  registerCleanup(() => deleteRecipe(recipe.id));

  await page.goto(`/recipes/${recipe.id}`);
  await expect(page.getByText(/no ingredients added/i)).toBeVisible();
});
