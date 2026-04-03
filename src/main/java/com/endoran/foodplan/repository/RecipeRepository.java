package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.Recipe;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RecipeRepository extends MongoRepository<Recipe, String> {

    List<Recipe> findByNameContainingIgnoreCase(String name);

    List<Recipe> findByOrgId(String orgId);

    List<Recipe> findByOrgIdAndNameContainingIgnoreCase(String orgId, String name);
}
