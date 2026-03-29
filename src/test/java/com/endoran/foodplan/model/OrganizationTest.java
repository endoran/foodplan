package com.endoran.foodplan.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationTest {

    @Test
    void settersAndGetters() {
        Organization org = new Organization();
        org.setId("org1");
        org.setName("Grace Church");
        org.setSettings(new OrgSettings("America/Denver", 20));

        assertEquals("org1", org.getId());
        assertEquals("Grace Church", org.getName());
        assertEquals("America/Denver", org.getSettings().getTimezone());
        assertEquals(20, org.getSettings().getDefaultServings());
    }

    @Test
    void newOrganizationHasDefaultSettings() {
        Organization org = new Organization();
        assertNotNull(org.getSettings());
        assertEquals("America/Chicago", org.getSettings().getTimezone());
        assertEquals(4, org.getSettings().getDefaultServings());
    }

    @Test
    void equalsSameId() {
        Organization a = new Organization();
        a.setId("org1");
        a.setName("Org A");

        Organization b = new Organization();
        b.setId("org1");
        b.setName("Different Name");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentIdNotEqual() {
        Organization a = new Organization();
        a.setId("org1");

        Organization b = new Organization();
        b.setId("org2");

        assertNotEquals(a, b);
    }
}
