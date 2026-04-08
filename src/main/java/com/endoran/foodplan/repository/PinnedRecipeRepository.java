package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.PinnedRecipe;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PinnedRecipeRepository extends MongoRepository<PinnedRecipe, String> {

    List<PinnedRecipe> findByOrgId(String orgId);

    Optional<PinnedRecipe> findByIdAndOrgId(String id, String orgId);

    Optional<PinnedRecipe> findByOrgIdAndSharedRecipeId(String orgId, String sharedRecipeId);

    void deleteByIdAndOrgId(String id, String orgId);
}
