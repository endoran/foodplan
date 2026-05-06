let counter = 0;

export function uniqueSuffix(): string {
  counter++;
  return `${Date.now()}-${counter}-${Math.random().toString(36).slice(2, 6)}`;
}

export function ingredientData(suffix?: string) {
  const s = suffix || uniqueSuffix();
  return {
    name: `[E2E] Ingredient ${s}`,
    storageCategory: 'PANTRY' as const,
    groceryCategory: 'SPICES' as const,
    dietaryTags: [] as string[],
    shoppingListExclude: false,
  };
}

export function recipeData(ingredientIds?: { ingredientId: string; ingredientName: string; quantity: number; unit: string }[], suffix?: string) {
  const s = suffix || uniqueSuffix();
  return {
    name: `[E2E] Recipe ${s}`,
    instructions: 'Combine all ingredients. Cook until done.',
    baseServings: 4,
    ingredients: ingredientIds || [],
  };
}

export function mealPlanData(recipeId: string, date?: string) {
  return {
    recipeId,
    date: date || todayISO(),
    mealType: 'DINNER',
    servings: 4,
  };
}

export function todayISO(): string {
  return new Date().toISOString().split('T')[0];
}

export function futureDateISO(daysAhead: number): string {
  const d = new Date();
  d.setDate(d.getDate() + daysAhead);
  return d.toISOString().split('T')[0];
}
