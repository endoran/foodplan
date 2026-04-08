package com.endoran.foodplan.controller;

import com.endoran.foodplan.dto.GlobalRecipeBookStatus;
import com.endoran.foodplan.dto.PinnedRecipeResponse;
import com.endoran.foodplan.dto.RecipeResponse;
import com.endoran.foodplan.dto.SharedRecipeResponse;
import com.endoran.foodplan.dto.WebRecipeSearchResult;
import com.endoran.foodplan.service.GlobalRecipeService;
import com.endoran.foodplan.service.RecipeNotFoundException;
import com.endoran.foodplan.service.WebRecipeSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/global-recipes")
public class GlobalRecipeController {

    private final GlobalRecipeService globalRecipeService;
    private final WebRecipeSearchService webRecipeSearchService;

    public GlobalRecipeController(GlobalRecipeService globalRecipeService,
                                  WebRecipeSearchService webRecipeSearchService) {
        this.globalRecipeService = globalRecipeService;
        this.webRecipeSearchService = webRecipeSearchService;
    }

    @GetMapping("/status")
    public ResponseEntity<GlobalRecipeBookStatus> status() {
        return ResponseEntity.ok(globalRecipeService.status());
    }

    @PostMapping("/share/{recipeId}")
    public ResponseEntity<SharedRecipeResponse> share(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String recipeId) {
        checkEnabled();
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(globalRecipeService.share(orgId, recipeId));
    }

    @DeleteMapping("/share/{recipeId}")
    public ResponseEntity<Void> unshare(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String recipeId) {
        checkEnabled();
        String orgId = jwt.getClaimAsString("orgId");
        globalRecipeService.unshare(orgId, recipeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<List<SharedRecipeResponse>> myShares(
            @AuthenticationPrincipal Jwt jwt) {
        checkEnabled();
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(globalRecipeService.myShares(orgId));
    }

    @GetMapping
    public ResponseEntity<List<SharedRecipeResponse>> browse(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        checkEnabled();
        return ResponseEntity.ok(globalRecipeService.browse(search, page, size));
    }

    @GetMapping("/web-search")
    public ResponseEntity<List<WebRecipeSearchResult>> webSearch(
            @RequestParam("q") String query) {
        checkEnabled();
        if (query == null || query.isBlank() || query.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(webRecipeSearchService.search(query));
    }

    @GetMapping("/{sharedId}")
    public ResponseEntity<SharedRecipeResponse> getShared(@PathVariable String sharedId) {
        checkEnabled();
        return ResponseEntity.ok(globalRecipeService.getShared(sharedId));
    }

    @PostMapping("/{sharedId}/pin")
    public ResponseEntity<PinnedRecipeResponse> pin(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sharedId) {
        checkEnabled();
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.status(HttpStatus.CREATED).body(globalRecipeService.pin(orgId, sharedId));
    }

    @DeleteMapping("/pin/{pinnedId}")
    public ResponseEntity<?> unpin(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String pinnedId,
            @RequestParam(defaultValue = "false") boolean cascade) {
        checkEnabled();
        String orgId = jwt.getClaimAsString("orgId");
        int calendarCount = globalRecipeService.unpinCalendarCount(orgId, pinnedId);
        if (calendarCount > 0 && !cascade) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("calendarEntryCount", calendarCount));
        }
        globalRecipeService.unpin(orgId, pinnedId, cascade);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pin/{pinnedId}/accept-update")
    public ResponseEntity<PinnedRecipeResponse> acceptUpdate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String pinnedId) {
        checkEnabled();
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(globalRecipeService.acceptUpdate(orgId, pinnedId));
    }

    @PostMapping("/pin/{pinnedId}/copy")
    public ResponseEntity<RecipeResponse> copyAsOwn(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String pinnedId) {
        checkEnabled();
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(globalRecipeService.copyAsOwn(orgId, pinnedId));
    }

    @GetMapping("/pins")
    public ResponseEntity<List<PinnedRecipeResponse>> listPins(
            @AuthenticationPrincipal Jwt jwt) {
        checkEnabled();
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(globalRecipeService.listPins(orgId));
    }

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RecipeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    private void checkEnabled() {
        if (!globalRecipeService.isEnabled()) {
            throw new GlobalRecipeBookDisabledException();
        }
    }

    public static class GlobalRecipeBookDisabledException extends RuntimeException {
        public GlobalRecipeBookDisabledException() {
            super("Global Recipe Book is not enabled");
        }
    }

    @ExceptionHandler(GlobalRecipeBookDisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(GlobalRecipeBookDisabledException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }
}
