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
}

export interface Recipe {
  id: string;
  name: string;
  instructions: string;
  baseServings: number;
  servings: number;
  ingredients: RecipeIngredient[];
}

export interface Ingredient {
  id: string;
  name: string;
  storageCategory: string;
  groceryCategory: string;
  dietaryTags: string[];
}

export interface MeasurementUnit {
  value: string;
}
