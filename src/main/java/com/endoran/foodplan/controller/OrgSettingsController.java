package com.endoran.foodplan.controller;

import com.endoran.foodplan.dto.OrgSettingsResponse;
import com.endoran.foodplan.dto.UpdateOrgSettingsRequest;
import com.endoran.foodplan.service.OrgSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
public class OrgSettingsController {

    private final OrgSettingsService orgSettingsService;

    public OrgSettingsController(OrgSettingsService orgSettingsService) {
        this.orgSettingsService = orgSettingsService;
    }

    @GetMapping
    public ResponseEntity<OrgSettingsResponse> getSettings(@AuthenticationPrincipal Jwt jwt) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(orgSettingsService.getSettings(orgId));
    }

    @PutMapping
    public ResponseEntity<OrgSettingsResponse> updateSettings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateOrgSettingsRequest request) {
        String orgId = jwt.getClaimAsString("orgId");
        return ResponseEntity.ok(orgSettingsService.updateSettings(orgId, request));
    }
}
