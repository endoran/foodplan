package com.endoran.foodplan.model;

import org.springframework.data.annotation.Id;

public class Ingredient {
    @Id
    private String id;
    private String name;
    private StorageCategory storageCategory;
    private GroceryCategory groceryCategory;

    public String getId() {
        return id;
    }

    public String getName() {
        return this.name;
    }

    public StorageCategory getStorageCategory() {
        return storageCategory;
    }

    public GroceryCategory getGroceryCategory() {
        return groceryCategory;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStorageCategory(StorageCategory storageCategory) {
        this.storageCategory = storageCategory;
    }

    public void setGroceryCategory(GroceryCategory groceryCategory) {
        this.groceryCategory = groceryCategory;
    }
}
