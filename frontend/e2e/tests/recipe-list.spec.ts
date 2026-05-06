import { test, expect } from '@playwright/test';
import { createTestRecipe, deleteRecipe } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('search filters recipe list', async ({ page }) => {
  const suffix = uniqueSuffix();
  const recipe = await createTestRecipe({ name: `[E2E] Waffles ${suffix}` });
  registerCleanup(() => deleteRecipe(recipe.id));

  await page.goto('/recipes');
  await expect(page.getByRole('heading', { name: 'Recipes' })).toBeVisible();

  const searchInput = page.locator('.search-bar input');
  await searchInput.fill(`[E2E] Waffles ${suffix}`);

  // Wait for debounced search
  await expect(page.locator('.card-grid .card').first()).toContainText(`[E2E] Waffles ${suffix}`, { timeout: 5000 });
});

test('click recipe card navigates to detail', async ({ page }) => {
  const suffix = uniqueSuffix();
  const recipe = await createTestRecipe({ name: `[E2E] Pasta ${suffix}` });
  registerCleanup(() => deleteRecipe(recipe.id));

  await page.goto('/recipes');
  const searchInput = page.locator('.search-bar input');
  await searchInput.fill(`[E2E] Pasta ${suffix}`);

  await page.locator('.card-grid a.card').first().click();
  await expect(page).toHaveURL(`/recipes/${recipe.id}`, { timeout: 10_000 });
});

test('new recipe button navigates to form', async ({ page }) => {
  await page.goto('/recipes');
  await page.getByRole('link', { name: /new recipe/i }).click();
  await expect(page).toHaveURL('/recipes/new');
  await expect(page.getByRole('heading', { name: 'New Recipe' })).toBeVisible();
});

test('empty search shows no recipes message', async ({ page }) => {
  await page.goto('/recipes');
  const searchInput = page.locator('.search-bar input');
  await searchInput.fill('zzz_nonexistent_recipe_xyz_' + Date.now());

  await expect(page.getByText(/no recipes found/i)).toBeVisible({ timeout: 5000 });
});

test('import URL link navigates to import page', async ({ page }) => {
  await page.goto('/recipes');
  await page.getByRole('link', { name: /import url/i }).click();
  await expect(page).toHaveURL('/recipes/import');
});
