package com.endoran.foodplan.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Document(collection = "ingredients")
public class Ingredient {

    @Id
    private String id;
    private String orgId;
    private String name;
    private StorageCategory storageCategory;
    private GroceryCategory groceryCategory;
    private Set<DietaryTag> dietaryTags = new HashSet<>();
    private boolean needsReview;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
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

    public Set<DietaryTag> getDietaryTags() {
        return dietaryTags;
    }

    public void setDietaryTags(Set<DietaryTag> dietaryTags) {
        this.dietaryTags = dietaryTags;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
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
