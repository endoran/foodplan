export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK';
export type MealStatus = 'PLANNED' | 'CONFIRMED' | 'SKIPPED';

export interface DietaryWarning {
  ingredientName: string;
  tags: string[];
}

export interface MealPlanEntry {
  id: string;
  date: string; // ISO date "YYYY-MM-DD"
  mealType: MealType;
  recipeId: string;
  recipeName: string;
  servings: number;
  notes: string | null;
  status: MealStatus;
  warnings: DietaryWarning[];
}

export interface CreateMealPlanRequest {
  date: string;
  mealType: MealType;
  recipeId: string;
  servings: number;
  notes?: string;
}

export type CalendarView = 'week' | 'month';

export interface DropData {
  recipeId: string;
  recipeName: string;
  entryId?: string; // present = move existing meal, absent = new recipe from sidebar
}
