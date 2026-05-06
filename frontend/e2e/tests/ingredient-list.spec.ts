import { test, expect } from '@playwright/test';
import { createTestIngredient, deleteIngredient } from './helpers/api';
import { registerCleanup, runCleanup } from './helpers/cleanup';
import { uniqueSuffix } from './helpers/fixtures';

test.afterEach(async () => { await runCleanup(); });

test('ingredient list page loads with search and table', async ({ page }) => {
  await page.goto('/ingredients');
  await expect(page.getByRole('heading', { name: 'Ingredients' })).toBeVisible();
  await expect(page.locator('.search-bar input')).toBeVisible();
  await expect(page.locator('.ingredients-table')).toBeVisible();
});

test('search filters ingredient list', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Turmeric ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto('/ingredients');
  await page.locator('.search-bar input').fill(`[E2E] Turmeric ${suffix}`);

  await expect(page.getByText(`[E2E] Turmeric ${suffix}`)).toBeVisible({ timeout: 5000 });
});

test('click ingredient navigates to edit page', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Oregano ${suffix}` });
  registerCleanup(() => deleteIngredient(ing.id));

  await page.goto('/ingredients');
  await page.locator('.search-bar input').fill(`[E2E] Oregano ${suffix}`);
  await page.getByRole('link', { name: `[E2E] Oregano ${suffix}` }).click();

  await expect(page).toHaveURL(new RegExp(`/ingredients/${ing.id}/edit`));
  await expect(page.getByRole('heading', { name: 'Edit Ingredient' })).toBeVisible();
});

test('delete ingredient removes from list', async ({ page }) => {
  const suffix = uniqueSuffix();
  const ing = await createTestIngredient({ name: `[E2E] Basil ${suffix}` });
  // Will be deleted via UI — no cleanup needed

  await page.goto('/ingredients');
  await page.locator('.search-bar input').fill(`[E2E] Basil ${suffix}`);
  await expect(page.getByText(`[E2E] Basil ${suffix}`)).toBeVisible({ timeout: 5000 });

  const row = page.getByText(`[E2E] Basil ${suffix}`).locator('..').locator('..');
  await row.getByRole('button', { name: /delete/i }).click();

  await expect(page.getByText(`[E2E] Basil ${suffix}`)).not.toBeVisible({ timeout: 5000 });
});

test('normalize button shows preview', async ({ page }) => {
  await page.goto('/ingredients');
  await page.getByRole('button', { name: /normalize names/i }).click();

  await expect(
    page.locator('.normalize-preview')
      .or(page.getByText(/already normalized/i))
  ).toBeVisible({ timeout: 10_000 });
});
