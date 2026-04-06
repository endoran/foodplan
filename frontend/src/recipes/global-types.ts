export interface SharedRecipeIngredient {
  ingredientName: string;
  quantity: number;
  unit: string;
  section: string | null;
}

export interface SharedRecipe {
  id: string;
  name: string;
  instructions: string;
  baseServings: number;
  ingredients: SharedRecipeIngredient[];
  attribution: string;
  sourceInstanceName: string;
  version: number;
  sharedAt: string;
  updatedAt: string;
  ownedByCurrentInstance: boolean;
}

export interface PinnedRecipe {
  id: string;
  sharedRecipeId: string;
  name: string;
  instructions: string;
  baseServings: number;
  ingredients: SharedRecipeIngredient[];
  attribution: string;
  sourceInstanceName: string;
  pinnedVersion: number;
  hasUpdate: boolean;
  latestVersion: number | null;
  sourceRemoved: boolean;
  pinnedAt: string;
}

export interface GlobalBookStatus {
  enabled: boolean;
  reachable: boolean;
}
