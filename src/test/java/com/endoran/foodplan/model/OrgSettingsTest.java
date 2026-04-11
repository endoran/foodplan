package com.endoran.foodplan.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrgSettingsTest {

    @Test
    void defaultConstructorSetsDefaults() {
        OrgSettings s = new OrgSettings();
        assertEquals("America/Chicago", s.getTimezone());
        assertEquals(4, s.getDefaultServings());
    }

    @Test
    void parameterizedConstructorSetsFields() {
        OrgSettings s = new OrgSettings("America/New_York", 8);
        assertEquals("America/New_York", s.getTimezone());
        assertEquals(8, s.getDefaultServings());
    }

    @Test
    void settersUpdateFields() {
        OrgSettings s = new OrgSettings();
        s.setTimezone("Europe/London");
        s.setDefaultServings(12);
        assertEquals("Europe/London", s.getTimezone());
        assertEquals(12, s.getDefaultServings());
    }

    @Test
    void equalSettingsAreEqual() {
        OrgSettings a = new OrgSettings("America/Chicago", 4);
        OrgSettings b = new OrgSettings("America/Chicago", 4);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentTimezoneNotEqual() {
        OrgSettings a = new OrgSettings("America/Chicago", 4);
        OrgSettings b = new OrgSettings("America/New_York", 4);
        assertNotEquals(a, b);
    }

    @Test
    void differentServingsNotEqual() {
        OrgSettings a = new OrgSettings("America/Chicago", 4);
        OrgSettings b = new OrgSettings("America/Chicago", 8);
        assertNotEquals(a, b);
    }

    @Test
    void equalsHandlesNullAndOtherTypes() {
        OrgSettings s = new OrgSettings();
        assertNotEquals(null, s);
        assertNotEquals("not settings", s);
    }
}
