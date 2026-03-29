package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.CreateInventoryItemRequest;
import com.endoran.foodplan.dto.InventoryItemResponse;
import com.endoran.foodplan.dto.UpdateInventoryItemRequest;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.InventoryItem;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final IngredientRepository ingredientRepository;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            IngredientRepository ingredientRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public InventoryItemResponse create(String orgId, CreateInventoryItemRequest request) {
        Ingredient ingredient = findIngredientByIdAndOrg(orgId, request.ingredientId());

        InventoryItem item = new InventoryItem();
        item.setOrgId(orgId);
        item.setIngredientId(ingredient.getId());
        item.setIngredientName(ingredient.getName());
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item = inventoryItemRepository.save(item);
        return toResponse(item);
    }

    public InventoryItemResponse getById(String orgId, String id) {
        InventoryItem item = findByIdAndOrg(orgId, id);
        return toResponse(item);
    }

    public List<InventoryItemResponse> list(String orgId) {
        return inventoryItemRepository.findByOrgId(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    public InventoryItemResponse update(String orgId, String id, UpdateInventoryItemRequest request) {
        InventoryItem item = findByIdAndOrg(orgId, id);
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item = inventoryItemRepository.save(item);
        return toResponse(item);
    }

    public void delete(String orgId, String id) {
        InventoryItem item = findByIdAndOrg(orgId, id);
        inventoryItemRepository.deleteById(item.getId());
    }

    private InventoryItem findByIdAndOrg(String orgId, String id) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new InventoryItemNotFoundException(id));
        if (!orgId.equals(item.getOrgId())) {
            throw new InventoryItemNotFoundException(id);
        }
        return item;
    }

    private Ingredient findIngredientByIdAndOrg(String orgId, String ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IngredientNotFoundException(ingredientId));
        if (!orgId.equals(ingredient.getOrgId())) {
            throw new IngredientNotFoundException(ingredientId);
        }
        return ingredient;
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getIngredientId(),
                item.getIngredientName(),
                item.getQuantity(),
                item.getUnit()
        );
    }
}
