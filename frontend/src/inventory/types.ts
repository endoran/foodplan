export interface InventoryItem {
  id: string;
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
}

export interface StoreProductAlternative {
  productId: string;
  productName: string;
  aisle: string;
  price: number | null;
  promoPrice: number | null;
  stockLevel: string | null;
  packageSize: string | null;
  qtyNeeded: number | null;
  totalPrice: number | null;
  totalPromoPrice: number | null;
}

export interface ShoppingItem {
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
  storeProducts?: StoreProductAlternative[];
}

export interface ShoppingAisle {
  category: string;
  items: ShoppingItem[];
}

export interface ShoppingListResponse {
  aisles: ShoppingAisle[];
  storeName?: string;
}
