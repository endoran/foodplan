package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.Ingredient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IngredientRepository extends MongoRepository<Ingredient, String> {

    List<Ingredient> findByNameContainingIgnoreCase(String name);

    List<Ingredient> findByGroceryCategory(GroceryCategory groceryCategory);

    List<Ingredient> findByOrgId(String orgId);

    List<Ingredient> findByOrgIdAndNameContainingIgnoreCase(String orgId, String name);

    List<Ingredient> findByOrgIdAndGroceryCategory(String orgId, GroceryCategory groceryCategory);
}
