package com.endoran.foodplan.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void settersAndGetters() {
        User u = new User();
        u.setId("u1");
        u.setEmail("pete@example.com");
        u.setPasswordHash("bcrypt_hash");
        u.setOauthProvider("google");
        u.setOauthId("google-12345");
        u.setOrgId("org1");
        u.setRole(UserRole.OWNER);

        assertEquals("u1", u.getId());
        assertEquals("pete@example.com", u.getEmail());
        assertEquals("bcrypt_hash", u.getPasswordHash());
        assertEquals("google", u.getOauthProvider());
        assertEquals("google-12345", u.getOauthId());
        assertEquals("org1", u.getOrgId());
        assertEquals(UserRole.OWNER, u.getRole());
    }

    @Test
    void nullableFieldsCanBeNull() {
        User u = new User();
        u.setId("u1");
        u.setEmail("pete@example.com");
        u.setOrgId("org1");
        u.setRole(UserRole.MEMBER);

        assertNull(u.getPasswordHash());
        assertNull(u.getOauthProvider());
        assertNull(u.getOauthId());
    }

    @Test
    void equalsSameIdAndEmail() {
        User a = new User();
        a.setId("u1");
        a.setEmail("a@b.com");

        User b = new User();
        b.setId("u1");
        b.setEmail("a@b.com");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentIdNotEqual() {
        User a = new User();
        a.setId("u1");
        a.setEmail("a@b.com");

        User b = new User();
        b.setId("u2");
        b.setEmail("a@b.com");

        assertNotEquals(a, b);
    }

    @Test
    void differentEmailNotEqual() {
        User a = new User();
        a.setId("u1");
        a.setEmail("a@b.com");

        User b = new User();
        b.setId("u1");
        b.setEmail("x@y.com");

        assertNotEquals(a, b);
    }
}
