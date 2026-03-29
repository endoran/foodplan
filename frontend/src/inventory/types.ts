export interface InventoryItem {
  id: string;
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
}

export interface ShoppingItem {
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
}

export interface ShoppingAisle {
  category: string;
  items: ShoppingItem[];
}

export interface ShoppingListResponse {
  aisles: ShoppingAisle[];
}
