package com.endoran.foodplan.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = "ingredients")
public class Ingredient {

    @Id
    private String id;
    private String name;
    private StorageCategory storageCategory;
    private GroceryCategory groceryCategory;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StorageCategory getStorageCategory() {
        return storageCategory;
    }

    public void setStorageCategory(StorageCategory storageCategory) {
        this.storageCategory = storageCategory;
    }

    public GroceryCategory getGroceryCategory() {
        return groceryCategory;
    }

    public void setGroceryCategory(GroceryCategory groceryCategory) {
        this.groceryCategory = groceryCategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
