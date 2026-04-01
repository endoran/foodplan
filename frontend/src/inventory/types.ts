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
  storeAisle?: string;
  storePrice?: number;
  storePromoPrice?: number;
  storeStockLevel?: string;
  storeProductName?: string;
}

export interface ShoppingAisle {
  category: string;
  items: ShoppingItem[];
}

export interface ShoppingListResponse {
  aisles: ShoppingAisle[];
  storeName?: string;
}
