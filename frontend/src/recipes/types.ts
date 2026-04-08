export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  email: string;
  orgId: string;
  role: string;
}

export interface RecipeIngredient {
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
  section: string | null;
}

export interface Recipe {
  id: string;
  name: string;
  instructions: string;
  baseServings: number;
  servings: number;
  ingredients: RecipeIngredient[];
  shared?: boolean;
}

export interface Ingredient {
  id: string;
  name: string;
  storageCategory: string;
  groceryCategory: string;
  dietaryTags: string[];
  needsReview: boolean;
  shoppingListExclude: boolean;
}

export interface MeasurementUnit {
  value: string;
}
