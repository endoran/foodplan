package com.endoran.foodplan.controller;

import com.endoran.foodplan.dto.CreateRecipeRequest;
import com.endoran.foodplan.dto.RecipeResponse;
import com.endoran.foodplan.dto.UpdateRecipeRequest;
import com.endoran.foodplan.service.RecipeNotFoundException;
import com.endoran.foodplan.service.RecipeService;
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
@RequestMapping("/api/v1/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @PostMapping
    public ResponseEntity<RecipeResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRecipeRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        RecipeResponse response = recipeService.create(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestParam(required = false) Integer servings) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(recipeService.getById(orgId, id, servings));
    }

    @GetMapping
    public ResponseEntity<List<RecipeResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String name) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(recipeService.list(orgId, name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody UpdateRecipeRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(recipeService.update(orgId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String orgId = jwt.getClaimAsString("orgId");
        recipeService.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RecipeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
