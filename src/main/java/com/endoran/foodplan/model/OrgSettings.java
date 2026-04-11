package com.endoran.foodplan.model;

import java.util.Objects;

public class OrgSettings {

    private String timezone;
    private int defaultServings;

    public OrgSettings() {
        this.timezone = "America/Chicago";
        this.defaultServings = 4;
    }

    public OrgSettings(String timezone, int defaultServings) {
        this.timezone = timezone;
        this.defaultServings = defaultServings;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgSettings that = (OrgSettings) o;
        return defaultServings == that.defaultServings && Objects.equals(timezone, that.timezone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timezone, defaultServings);
    }
}
