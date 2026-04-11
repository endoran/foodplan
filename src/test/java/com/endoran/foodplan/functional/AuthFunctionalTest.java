package com.endoran.foodplan.functional;

import com.endoran.foodplan.dto.AuthResponse;
import com.endoran.foodplan.dto.LoginRequest;
import com.endoran.foodplan.dto.RegisterRequest;
import com.endoran.foodplan.repository.OrganizationRepository;
import com.endoran.foodplan.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void registerReturnsJwtAndCreatesOrgAndUser() throws Exception {
        RegisterRequest request = new RegisterRequest("chef@example.com", "password123", "Pete's Kitchen");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.email").value("chef@example.com"))
                .andExpect(jsonPath("$.orgId").isNotEmpty())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn();

        // Verify user and org were persisted
        assertEquals(1, userRepository.count());
        assertEquals(1, organizationRepository.count());
        assertTrue(userRepository.findByEmail("chef@example.com").isPresent());
    }

    @Test
    void loginWithCorrectCredentialsReturnsJwt() throws Exception {
        // Register first
        register("chef@example.com", "password123", "Test Kitchen");

        // Login
        LoginRequest loginRequest = new LoginRequest("chef@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("chef@example.com"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        register("chef@example.com", "password123", "Test Kitchen");

        LoginRequest loginRequest = new LoginRequest("chef@example.com", "wrongpassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidTokenReturns200() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("chef@example.com"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.orgId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty());
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        register("chef@example.com", "password123", "Test Kitchen");

        RegisterRequest duplicate = new RegisterRequest("chef@example.com", "otherpass", "Other Kitchen");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("already registered")));
    }

    @Test
    void publicEndpointsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/reference/measurement-units"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // -- helpers --

    private void register(String email, String password, String orgName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, orgName);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String registerAndGetToken(String email, String password, String orgName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, orgName);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.accessToken();
    }
}
