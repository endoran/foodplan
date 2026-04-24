package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.OrgSettingsResponse;
import com.endoran.foodplan.dto.UpdateOrgSettingsRequest;
import com.endoran.foodplan.model.Organization;
import com.endoran.foodplan.model.OrgSettings;
import com.endoran.foodplan.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrgSettingsService {

    private final OrganizationRepository organizationRepository;

    public OrgSettingsService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public OrgSettingsResponse getSettings(String orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        OrgSettings settings = org.getSettings();
        if (settings == null) {
            settings = new OrgSettings();
        }
        List<String> sites = settings.getAllowedRecipeSites();
        if (sites == null) {
            sites = OrgSettings.DEFAULT_RECIPE_SITES;
        }
        return new OrgSettingsResponse(
                settings.getTimezone(),
                settings.getDefaultServings(),
                sites,
                OrgSettings.DEFAULT_RECIPE_SITES
        );
    }

    public OrgSettingsResponse updateSettings(String orgId, UpdateOrgSettingsRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        OrgSettings settings = org.getSettings();
        if (settings == null) {
            settings = new OrgSettings();
            org.setSettings(settings);
        }
        if (request.timezone() != null) {
            settings.setTimezone(request.timezone());
        }
        if (request.defaultServings() != null) {
            settings.setDefaultServings(request.defaultServings());
        }
        if (request.allowedRecipeSites() != null) {
            settings.setAllowedRecipeSites(request.allowedRecipeSites());
        }
        organizationRepository.save(org);
        return getSettings(orgId);
    }

    public List<String> getAllowedRecipeSites(String orgId) {
        Organization org = organizationRepository.findById(orgId).orElse(null);
        if (org == null || org.getSettings() == null) {
            return OrgSettings.DEFAULT_RECIPE_SITES;
        }
        List<String> sites = org.getSettings().getAllowedRecipeSites();
        if (sites == null) {
            return OrgSettings.DEFAULT_RECIPE_SITES;
        }
        return sites;
    }
}
