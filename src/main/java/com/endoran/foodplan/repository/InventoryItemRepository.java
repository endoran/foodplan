package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.InventoryItem;
import com.endoran.foodplan.model.MeasurementUnit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends MongoRepository<InventoryItem, String> {

    List<InventoryItem> findByOrgId(String orgId);

    Optional<InventoryItem> findByOrgIdAndIngredientIdAndUnit(String orgId, String ingredientId, MeasurementUnit unit);
}
