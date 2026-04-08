package com.endoran.foodplan.controller;

import com.endoran.foodplan.dto.CreateInventoryItemRequest;
import com.endoran.foodplan.dto.DeductInventoryItemRequest;
import com.endoran.foodplan.dto.InventoryItemResponse;
import com.endoran.foodplan.dto.UpdateInventoryItemRequest;
import com.endoran.foodplan.service.IngredientNotFoundException;
import com.endoran.foodplan.service.InventoryItemNotFoundException;
import com.endoran.foodplan.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryItemResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateInventoryItemRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        InventoryItemResponse response = inventoryService.create(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItemResponse> getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(inventoryService.getById(orgId, id));
    }

    @GetMapping
    public ResponseEntity<List<InventoryItemResponse>> list(
            @AuthenticationPrincipal Jwt jwt) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(inventoryService.list(orgId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItemResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody UpdateInventoryItemRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(inventoryService.update(orgId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String orgId = jwt.getClaimAsString("orgId");
        inventoryService.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/deduct")
    public ResponseEntity<Void> deduct(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody List<DeductInventoryItemRequest> items) {
        String orgId = jwt.getClaimAsString("orgId");
        inventoryService.deduct(orgId, items);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(InventoryItemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(InventoryItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IngredientNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleIngredientNotFound(IngredientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
