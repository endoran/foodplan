import { test, expect } from '@playwright/test';
import { createTestIngredient, createTestRecipe, deleteIngredient, deleteRecipe } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('create recipe end-to-end with ingredient', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Flour ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto('/recipes/new');
  await expect(page.getByRole('heading', { name: 'New Recipe' })).toBeVisible();

  const recipeName = `[E2E] Bread ${suffix}`;
  await page.locator('label:has-text("Name") input').fill(recipeName);
  await page.locator('label:has-text("Instructions") textarea').fill('Mix and bake.');
  await page.locator('label:has-text("Base Servings") input').fill('6');

  // Add ingredient via autocomplete
  await page.getByRole('button', { name: /add ingredient/i }).click();
  const autocomplete = page.locator('.autocomplete-wrap input').last();
  await autocomplete.fill(ing.name.substring(5)); // type partial name
  await page.locator('.autocomplete-option').first().click({ timeout: 5000 });

  // Fill quantity
  await page.locator('.ingredient-row input[placeholder*="Qty"]').last().fill('2');
  await page.locator('.ingredient-row select').last().selectOption('CUP');

  await page.getByRole('button', { name: /create recipe/i }).click();
  await expect(page).toHaveURL(/\/recipes\/[a-z0-9]+$/, { timeout: 10_000 });

  // Verify on detail page
  await expect(page.getByRole('heading', { name: recipeName })).toBeVisible();
  await expect(page.getByText(ing.name)).toBeVisible();

  // Cleanup: extract recipe ID from URL
  const recipeId = page.url().split('/').pop() || '';
  registerCleanup(() => deleteRecipe(recipeId));
});

test('edit recipe changes name', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Sugar ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));
  const recipe = await createTestRecipe({
    name: `[E2E] Cake ${suffix}`,
    ingredients: [{ ingredientId: ing.id, ingredientName: ing.name, quantity: 1, unit: 'CUP' }],
  });
  registerCleanup(() => deleteRecipe(recipe.id));

  await page.goto(`/recipes/${recipe.id}/edit`);
  await expect(page.getByRole('heading', { name: 'Edit Recipe' })).toBeVisible();

  const newName = `[E2E] Updated Cake ${suffix}`;
  await page.locator('label:has-text("Name") input').fill(newName);
  await page.getByRole('button', { name: /save changes/i }).click();

  await expect(page).toHaveURL(`/recipes/${recipe.id}`, { timeout: 10_000 });
  await expect(page.getByRole('heading', { name: newName })).toBeVisible();
});

test('fraction quantity inputs accepted', async ({ page }) => {
  await page.goto('/recipes/new');
  await page.getByRole('button', { name: /add ingredient/i }).click();

  const qty = page.locator('.ingredient-row input[placeholder*="Qty"]').last();
  await qty.fill('1/2');
  await expect(qty).toHaveValue('1/2');

  await qty.fill('1 1/2');
  await expect(qty).toHaveValue('1 1/2');

  await qty.fill('2.5');
  await expect(qty).toHaveValue('2.5');
});

test('ingredient autocomplete appears on type', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Cinnamon ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto('/recipes/new');
  await page.getByRole('button', { name: /add ingredient/i }).click();

  const autocomplete = page.locator('.autocomplete-wrap input').last();
  await autocomplete.fill('[E2E] Cinnamon');
  await expect(page.locator('.autocomplete-dropdown')).toBeVisible({ timeout: 5000 });
  await expect(page.locator('.autocomplete-option').first()).toContainText('Cinnamon');
});

test('validation rejects invalid servings', async ({ page }) => {
  await page.goto('/recipes/new');

  await page.locator('label:has-text("Name") input').fill('[E2E] Temp Recipe');
  await page.locator('label:has-text("Base Servings") input').fill('0');
  await page.getByRole('button', { name: /create recipe/i }).click();

  await expect(page.locator('.error')).toContainText('servings must be at least 1');
});
