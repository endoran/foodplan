import { apiGet, apiPost, apiDelete } from './client';
import type { GlobalBookStatus, SharedRecipe, PinnedRecipe, WebRecipeResult } from '../recipes/global-types';

export function getGlobalBookStatus(): Promise<GlobalBookStatus> {
  return apiGet('/api/v1/global-recipes/status');
}

export function shareRecipe(recipeId: string): Promise<SharedRecipe> {
  return apiPost('/api/v1/global-recipes/share/' + recipeId, {});
}

export function unshareRecipe(recipeId: string): Promise<void> {
  return apiDelete('/api/v1/global-recipes/share/' + recipeId);
}

export function getMyShares(): Promise<SharedRecipe[]> {
  return apiGet('/api/v1/global-recipes/mine');
}

export function browseGlobalRecipes(search?: string, page = 0, size = 20): Promise<SharedRecipe[]> {
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  params.set('page', String(page));
  params.set('size', String(size));
  return apiGet('/api/v1/global-recipes?' + params.toString());
}

export function getSharedRecipe(sharedId: string): Promise<SharedRecipe> {
  return apiGet('/api/v1/global-recipes/' + sharedId);
}

export function pinRecipe(sharedId: string): Promise<PinnedRecipe> {
  return apiPost('/api/v1/global-recipes/' + sharedId + '/pin', {});
}

export function unpinRecipe(pinnedId: string): Promise<void> {
  return apiDelete('/api/v1/global-recipes/pin/' + pinnedId);
}

export function acceptPinUpdate(pinnedId: string): Promise<PinnedRecipe> {
  return apiPost('/api/v1/global-recipes/pin/' + pinnedId + '/accept-update', {});
}

export function getMyPins(): Promise<PinnedRecipe[]> {
  return apiGet('/api/v1/global-recipes/pins');
}

export function searchWebRecipes(query: string): Promise<WebRecipeResult[]> {
  return apiGet('/api/v1/global-recipes/web-search?q=' + encodeURIComponent(query));
}
