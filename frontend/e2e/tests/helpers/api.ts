import { readFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const AUTH_FILE = join(__dirname, '..', '..', '.auth', 'user.json');

function getBaseUrl(): string {
  return process.env.BASE_URL || 'http://localhost:9090';
}

function getToken(): string {
  const raw = readFileSync(AUTH_FILE, 'utf-8');
  const state = JSON.parse(raw);
  const origins = state.origins || [];
  for (const origin of origins) {
    for (const entry of origin.localStorage || []) {
      if (entry.name === 'token') return entry.value;
    }
  }
  throw new Error('No auth token found in .auth/user.json — run global-setup first');
}

async function apiRequest<T>(method: string, path: string, body?: unknown): Promise<T> {
  const url = `${getBaseUrl()}${path}`;
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${getToken()}`,
  };

  const resp = await fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (resp.status === 204) return undefined as T;
  if (!resp.ok) {
    const text = await resp.text().catch(() => '');
    throw new Error(`API ${method} ${path} failed: ${resp.status} ${text}`);
  }
  return resp.json();
}

export interface CreatedIngredient {
  id: string;
  name: string;
}

export interface CreatedRecipe {
  id: string;
  name: string;
}

export interface CreatedMealPlanEntry {
  id: string;
}

export interface CreatedInventoryItem {
  id: string;
}

export async function createTestIngredient(overrides?: Partial<{
  name: string;
  storageCategory: string;
  groceryCategory: string;
  dietaryTags: string[];
  shoppingListExclude: boolean;
}>): Promise<CreatedIngredient> {
  return apiRequest('POST', '/api/v1/ingredients', {
    name: overrides?.name || `[E2E] Ingredient ${Date.now()}`,
    storageCategory: overrides?.storageCategory || 'PANTRY',
    groceryCategory: overrides?.groceryCategory || 'SPICES',
    dietaryTags: overrides?.dietaryTags || [],
    shoppingListExclude: overrides?.shoppingListExclude || false,
  });
}

export async function deleteIngredient(id: string): Promise<void> {
  await apiRequest('DELETE', `/api/v1/ingredients/${id}`).catch(() => {});
}

export async function createTestRecipe(overrides?: Partial<{
  name: string;
  instructions: string;
  baseServings: number;
  ingredients: { ingredientId: string; ingredientName: string; quantity: number; unit: string }[];
}>): Promise<CreatedRecipe> {
  return apiRequest('POST', '/api/v1/recipes', {
    name: overrides?.name || `[E2E] Recipe ${Date.now()}`,
    instructions: overrides?.instructions || 'Test instructions',
    baseServings: overrides?.baseServings || 4,
    ingredients: overrides?.ingredients || [],
  });
}

export async function deleteRecipe(id: string): Promise<void> {
  await apiRequest('DELETE', `/api/v1/recipes/${id}`).catch(() => {});
}

export async function createMealPlanEntry(data: {
  recipeId: string;
  date: string;
  mealType: string;
  servings: number;
}): Promise<CreatedMealPlanEntry> {
  return apiRequest('POST', '/api/v1/meal-plan', data);
}

export async function deleteMealPlanEntry(id: string): Promise<void> {
  await apiRequest('DELETE', `/api/v1/meal-plan/${id}`).catch(() => {});
}

export async function createInventoryItem(data: {
  ingredientId: string;
  quantity: number;
  unit: string;
}): Promise<CreatedInventoryItem> {
  return apiRequest('POST', '/api/v1/inventory', data);
}

export async function deleteInventoryItem(id: string): Promise<void> {
  await apiRequest('DELETE', `/api/v1/inventory/${id}`).catch(() => {});
}

export async function getGlobalBookStatus(): Promise<{ enabled: boolean; reachable: boolean }> {
  return apiRequest('GET', '/api/v1/global-recipes/status');
}

export async function getSettings(): Promise<Record<string, unknown>> {
  return apiRequest('GET', '/api/v1/settings');
}

export async function updateSettings(body: Record<string, unknown>): Promise<void> {
  await apiRequest('PUT', '/api/v1/settings', body);
}
