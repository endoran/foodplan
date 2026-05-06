import { test, expect } from '@playwright/test';
import { createTestIngredient, createTestRecipe, createMealPlanEntry, deleteIngredient, deleteRecipe, deleteMealPlanEntry } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix, todayISO, futureDateISO } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('calendar page loads with navigation controls', async ({ page }) => {
  await page.goto('/calendar');
  await expect(page.getByRole('heading', { name: /meal calendar/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /prev/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /next/i })).toBeVisible();
  await expect(page.getByRole('button', { name: /today/i })).toBeVisible();
});

test('prev/next buttons change displayed date range', async ({ page }) => {
  await page.goto('/calendar');
  const title = page.locator('.calendar-title');
  const initialText = await title.textContent();

  await page.getByRole('button', { name: /next/i }).click();
  await expect(title).not.toHaveText(initialText!);

  await page.getByRole('button', { name: /prev/i }).click();
  await expect(title).toHaveText(initialText!);
});

test('week/month view toggle works', async ({ page }) => {
  await page.goto('/calendar');

  await page.getByRole('button', { name: /month/i }).click();
  await expect(page.locator('.month-view')).toBeVisible({ timeout: 5000 });

  await page.getByRole('button', { name: /week/i }).click();
  await expect(page.locator('.week-grid')).toBeVisible({ timeout: 5000 });
});

test('meal plan entry displays on calendar', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Tomato ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));
  const recipe = await createTestRecipe({
    name: `[E2E] Soup ${suffix}`,
    ingredients: [{ ingredientId: ing.id, ingredientName: ing.name, quantity: 2, unit: 'CUP' }],
  });
  registerCleanup(() => deleteRecipe(recipe.id));

  const entry = await createMealPlanEntry({ recipeId: recipe.id, date: todayISO(), mealType: 'DINNER', servings: 4 });
  registerCleanup(() => deleteMealPlanEntry(entry.id));

  await page.goto('/calendar');
  await page.getByRole('button', { name: /today/i }).click();
  await expect(page.getByText(`[E2E] Soup ${suffix}`)).toBeVisible({ timeout: 5000 });
});

test('delete meal entry removes it from calendar', async ({ page }) => {
  const suffix = uniqueSuffix();
  const recipe = await createTestRecipe({ name: `[E2E] Delete Meal ${suffix}` });
  registerCleanup(() => deleteRecipe(recipe.id));

  const entry = await createMealPlanEntry({ recipeId: recipe.id, date: todayISO(), mealType: 'LUNCH', servings: 2 });
  // Entry will be deleted by the UI action — no cleanup needed for it

  await page.goto('/calendar');
  await page.getByRole('button', { name: /today/i }).click();
  const mealText = page.getByText(`[E2E] Delete Meal ${suffix}`);
  await expect(mealText).toBeVisible({ timeout: 5000 });

  // Click delete on the meal entry
  const mealEntry = mealText.locator('..').locator('..');
  await mealEntry.locator('.btn-icon-danger, .btn-icon').last().click();
  await expect(mealText).not.toBeVisible({ timeout: 5000 });
});
