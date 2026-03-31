package com.endoran.foodplan.functional;

import com.endoran.foodplan.dto.AuthResponse;
import com.endoran.foodplan.dto.CreateIngredientRequest;
import com.endoran.foodplan.dto.RegisterRequest;
import com.endoran.foodplan.dto.UpdateIngredientRequest;
import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.StorageCategory;
import com.endoran.foodplan.repository.IngredientRepository;
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

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IngredientFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @AfterEach
    void cleanup() {
        ingredientRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void createIngredientReturns201WithBody() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        CreateIngredientRequest request = new CreateIngredientRequest(
                "Cheddar Cheese", StorageCategory.REFRIGERATED, GroceryCategory.DAIRY,
                Set.of(DietaryTag.VEGETARIAN), false);

        mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Cheddar Cheese"))
                .andExpect(jsonPath("$.storageCategory").value("REFRIGERATED"))
                .andExpect(jsonPath("$.groceryCategory").value("DAIRY"))
                .andExpect(jsonPath("$.dietaryTags", hasItem("VEGETARIAN")));
    }

    @Test
    void createIngredientWithoutTokenReturns401() throws Exception {
        CreateIngredientRequest request = new CreateIngredientRequest(
                "Salt", StorageCategory.DRY, GroceryCategory.BAKING, null, false);

        mockMvc.perform(post("/api/v1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createIngredientWithInvalidBodyReturns400() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");

        mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"storageCategory\": null, \"groceryCategory\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIngredientByIdReturns200() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String id = createIngredient(token, "Cheddar Cheese", StorageCategory.REFRIGERATED, GroceryCategory.DAIRY, null);

        mockMvc.perform(get("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Cheddar Cheese"));
    }

    @Test
    void getIngredientFromOtherOrgReturns404() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String id = createIngredient(tokenA, "Secret Spice", StorageCategory.DRY, GroceryCategory.BAKING, null);

        mockMvc.perform(get("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void listIngredientsScopedToOrg() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");

        createIngredient(tokenA, "Cheddar Cheese", StorageCategory.REFRIGERATED, GroceryCategory.DAIRY, null);
        createIngredient(tokenA, "Ground Beef", StorageCategory.REFRIGERATED, GroceryCategory.MEAT, null);
        createIngredient(tokenB, "Romaine Lettuce", StorageCategory.FRESH, GroceryCategory.PRODUCE, null);

        mockMvc.perform(get("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listIngredientsFilterByName() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        createIngredient(token, "Cheddar Cheese", StorageCategory.REFRIGERATED, GroceryCategory.DAIRY, null);
        createIngredient(token, "Romaine Lettuce", StorageCategory.FRESH, GroceryCategory.PRODUCE, null);

        mockMvc.perform(get("/api/v1/ingredients")
                        .param("name", "cheese")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cheddar Cheese"));
    }

    @Test
    void listIngredientsFilterByGroceryCategory() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        createIngredient(token, "Cheddar Cheese", StorageCategory.REFRIGERATED, GroceryCategory.DAIRY, null);
        createIngredient(token, "Romaine Lettuce", StorageCategory.FRESH, GroceryCategory.PRODUCE, null);

        mockMvc.perform(get("/api/v1/ingredients")
                        .param("groceryCategory", "DAIRY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cheddar Cheese"));
    }

    @Test
    void listIngredientsFilterByDietaryTag() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        createIngredient(token, "Tofu", StorageCategory.REFRIGERATED, GroceryCategory.PRODUCE,
                Set.of(DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE));
        createIngredient(token, "Ground Beef", StorageCategory.REFRIGERATED, GroceryCategory.MEAT, null);

        mockMvc.perform(get("/api/v1/ingredients")
                        .param("dietaryTag", "GLUTEN_FREE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Tofu"));
    }

    @Test
    void updateIngredientReturns200() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String id = createIngredient(token, "Chedder Cheese", StorageCategory.REFRIGERATED, GroceryCategory.DAIRY, null);

        UpdateIngredientRequest update = new UpdateIngredientRequest(
                "Cheddar Cheese", StorageCategory.REFRIGERATED, GroceryCategory.DAIRY,
                Set.of(DietaryTag.VEGETARIAN), false);

        mockMvc.perform(put("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cheddar Cheese"))
                .andExpect(jsonPath("$.dietaryTags", hasItem("VEGETARIAN")));
    }

    @Test
    void updateIngredientFromOtherOrgReturns404() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String id = createIngredient(tokenA, "Secret Spice", StorageCategory.DRY, GroceryCategory.BAKING, null);

        UpdateIngredientRequest update = new UpdateIngredientRequest(
                "Stolen Spice", StorageCategory.DRY, GroceryCategory.BAKING, null, false);

        mockMvc.perform(put("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIngredientReturns204ThenGetReturns404() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String id = createIngredient(token, "Cheddar Cheese", StorageCategory.REFRIGERATED, GroceryCategory.DAIRY, null);

        mockMvc.perform(delete("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteIngredientFromOtherOrgReturns404() throws Exception {
        String tokenA = registerAndGetToken("chefA@example.com", "password123", "Kitchen A");
        String tokenB = registerAndGetToken("chefB@example.com", "password123", "Kitchen B");
        String id = createIngredient(tokenA, "Secret Spice", StorageCategory.DRY, GroceryCategory.BAKING, null);

        mockMvc.perform(delete("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void createIngredientWithDietaryTagsRoundTrips() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String id = createIngredient(token, "Tofu", StorageCategory.REFRIGERATED, GroceryCategory.PRODUCE,
                Set.of(DietaryTag.GLUTEN_FREE, DietaryTag.DAIRY_FREE, DietaryTag.VEGAN));

        mockMvc.perform(get("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dietaryTags", hasSize(3)))
                .andExpect(jsonPath("$.dietaryTags", hasItem("GLUTEN_FREE")))
                .andExpect(jsonPath("$.dietaryTags", hasItem("DAIRY_FREE")))
                .andExpect(jsonPath("$.dietaryTags", hasItem("VEGAN")));
    }

    @Test
    void createIngredientWithNullDietaryTagsDefaultsToEmpty() throws Exception {
        String token = registerAndGetToken("chef@example.com", "password123", "Test Kitchen");
        String id = createIngredient(token, "Salt", StorageCategory.DRY, GroceryCategory.BAKING, null);

        mockMvc.perform(get("/api/v1/ingredients/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dietaryTags", hasSize(0)));
    }

    // -- helpers --

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

    private String createIngredient(String token, String name, StorageCategory storage,
                                    GroceryCategory grocery, Set<DietaryTag> tags) throws Exception {
        CreateIngredientRequest request = new CreateIngredientRequest(name, storage, grocery, tags, false);
        MvcResult result = mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
