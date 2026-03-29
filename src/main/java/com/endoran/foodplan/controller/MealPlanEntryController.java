package com.endoran.foodplan.controller;

import com.endoran.foodplan.dto.CreateMealPlanEntryRequest;
import com.endoran.foodplan.dto.MealPlanEntryResponse;
import com.endoran.foodplan.dto.UpdateMealPlanEntryRequest;
import com.endoran.foodplan.model.MealType;
import com.endoran.foodplan.service.MealPlanEntryNotFoundException;
import com.endoran.foodplan.service.MealPlanEntryService;
import com.endoran.foodplan.service.RecipeNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/meal-plan")
public class MealPlanEntryController {

    private final MealPlanEntryService mealPlanEntryService;

    public MealPlanEntryController(MealPlanEntryService mealPlanEntryService) {
        this.mealPlanEntryService = mealPlanEntryService;
    }

    @PostMapping
    public ResponseEntity<MealPlanEntryResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMealPlanEntryRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        MealPlanEntryResponse response = mealPlanEntryService.create(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealPlanEntryResponse> getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(mealPlanEntryService.getById(orgId, id));
    }

    @GetMapping
    public ResponseEntity<List<MealPlanEntryResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) MealType mealType) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(mealPlanEntryService.list(orgId, from, to, mealType));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealPlanEntryResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody UpdateMealPlanEntryRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(mealPlanEntryService.update(orgId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String orgId = jwt.getClaimAsString("orgId");
        mealPlanEntryService.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<MealPlanEntryResponse> confirm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(mealPlanEntryService.confirm(orgId, id));
    }

    @ExceptionHandler(MealPlanEntryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(MealPlanEntryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRecipeNotFound(RecipeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
