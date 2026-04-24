package com.endoran.foodplan.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrgSettings {

    public static final List<String> DEFAULT_RECIPE_SITES = List.of(
            "allrecipes.com", "food.com", "foodnetwork.com",
            "simplyrecipes.com", "budgetbytes.com", "seriouseats.com",
            "epicurious.com", "bonappetit.com", "delish.com",
            "tasty.co", "cookieandkate.com", "minimalistbaker.com"
    );

    private String timezone;
    private int defaultServings;
    private List<String> allowedRecipeSites;

    public OrgSettings() {
        this.timezone = "America/Chicago";
        this.defaultServings = 4;
        this.allowedRecipeSites = new ArrayList<>(DEFAULT_RECIPE_SITES);
    }

    public OrgSettings(String timezone, int defaultServings) {
        this(timezone, defaultServings, null);
    }

    public OrgSettings(String timezone, int defaultServings, List<String> allowedRecipeSites) {
        this.timezone = timezone;
        this.defaultServings = defaultServings;
        this.allowedRecipeSites = allowedRecipeSites != null ? new ArrayList<>(allowedRecipeSites) : new ArrayList<>(DEFAULT_RECIPE_SITES);
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getDefaultServings() {
        return defaultServings;
    }

    public void setDefaultServings(int defaultServings) {
        this.defaultServings = defaultServings;
    }

    public List<String> getAllowedRecipeSites() {
        return allowedRecipeSites;
    }

    public void setAllowedRecipeSites(List<String> allowedRecipeSites) {
        this.allowedRecipeSites = allowedRecipeSites;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgSettings that = (OrgSettings) o;
        return defaultServings == that.defaultServings
                && Objects.equals(timezone, that.timezone)
                && Objects.equals(allowedRecipeSites, that.allowedRecipeSites);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timezone, defaultServings, allowedRecipeSites);
    }
}
