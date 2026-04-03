package com.endoran.foodplan.controller;

import com.endoran.foodplan.dto.BatchCreateIngredientsRequest;
import com.endoran.foodplan.dto.BulkUpdateIngredientsRequest;
import com.endoran.foodplan.dto.CreateIngredientRequest;
import com.endoran.foodplan.dto.IngredientPreparation;
import com.endoran.foodplan.dto.IngredientResponse;
import com.endoran.foodplan.dto.PrepareIngredientsRequest;
import com.endoran.foodplan.dto.UpdateIngredientRequest;
import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.service.IngredientNotFoundException;
import com.endoran.foodplan.service.IngredientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping
    public ResponseEntity<IngredientResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateIngredientRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        IngredientResponse response = ingredientService.create(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/prepare")
    public ResponseEntity<List<IngredientPreparation>> prepare(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PrepareIngredientsRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(ingredientService.prepareIngredients(orgId, request.ingredientNames()));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<IngredientResponse>> batchCreate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BatchCreateIngredientsRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ingredientService.batchCreate(orgId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientResponse> getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(ingredientService.getById(orgId, id));
    }

    @GetMapping
    public ResponseEntity<List<IngredientResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) GroceryCategory groceryCategory,
            @RequestParam(required = false) DietaryTag dietaryTag) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(ingredientService.list(orgId, name, groceryCategory, dietaryTag));
    }

    @PutMapping("/bulk")
    public ResponseEntity<List<IngredientResponse>> bulkUpdate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BulkUpdateIngredientsRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(ingredientService.bulkUpdate(orgId, request));
    }

    @PostMapping("/auto-categorize")
    public ResponseEntity<List<IngredientResponse>> autoCategorize(
            @AuthenticationPrincipal Jwt jwt) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(ingredientService.autoCategorize(orgId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody UpdateIngredientRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(ingredientService.update(orgId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String orgId = jwt.getClaimAsString("orgId");
        ingredientService.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IngredientNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IngredientNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
