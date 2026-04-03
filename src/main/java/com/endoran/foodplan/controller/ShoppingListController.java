package com.endoran.foodplan.controller;

import com.endoran.foodplan.dto.ShoppingListResponse;
import com.endoran.foodplan.model.StoreType;
import com.endoran.foodplan.service.ShoppingListService;
import com.endoran.foodplan.service.StoreEnrichmentOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/shopping-list")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final StoreEnrichmentOrchestrator storeEnrichment;

    public ShoppingListController(ShoppingListService shoppingListService,
                                   StoreEnrichmentOrchestrator storeEnrichment) {
        this.shoppingListService = shoppingListService;
        this.storeEnrichment = storeEnrichment;
    }

    @GetMapping
    public ResponseEntity<ShoppingListResponse> generate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) StoreType store) {
        String orgId = jwt.getClaimAsString("orgId");
        ShoppingListResponse response = shoppingListService.generate(orgId, from, to);
        if (store != null) {
            response = storeEnrichment.enrich(response, store);
        }
        return ResponseEntity.ok(response);
    }
}
