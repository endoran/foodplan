package com.endoran.foodplan.functional;

import com.endoran.foodplan.model.*;
import com.endoran.foodplan.repository.IngredientRepository;
import com.endoran.foodplan.repository.RecipeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @AfterEach
    void cleanup() {
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
    }

    @Test
    void applicationContextLoads() {
        // If we get here, the full Spring context + embedded Mongo booted successfully
    }

    @Test
    void createAndRetrieveIngredient() {
        Ingredient cheese = new Ingredient();
        cheese.setName("Cheddar Cheese");
        cheese.setGroceryCategory(GroceryCategory.DAIRY);
        cheese.setStorageCategory(StorageCategory.REFRIGERATED);

        Ingredient saved = ingredientRepository.save(cheese);

        assertNotNull(saved.getId());
        assertEquals("Cheddar Cheese", saved.getName());
        assertEquals(GroceryCategory.DAIRY, saved.getGroceryCategory());

        // Retrieve by ID
        Ingredient found = ingredientRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Cheddar Cheese", found.getName());
    }

    @Test
    void ingredientSearchByName() {
        Ingredient cheddar = new Ingredient();
        cheddar.setName("Cheddar Cheese");
        cheddar.setGroceryCategory(GroceryCategory.DAIRY);
        cheddar.setStorageCategory(StorageCategory.REFRIGERATED);

        Ingredient mozzarella = new Ingredient();
        mozzarella.setName("Mozzarella Cheese");
        mozzarella.setGroceryCategory(GroceryCategory.DAIRY);
        mozzarella.setStorageCategory(StorageCategory.REFRIGERATED);

        Ingredient lettuce = new Ingredient();
        lettuce.setName("Romaine Lettuce");
        lettuce.setGroceryCategory(GroceryCategory.PRODUCE);
        lettuce.setStorageCategory(StorageCategory.FRESH);

        ingredientRepository.saveAll(List.of(cheddar, mozzarella, lettuce));

        // Search by name fragment
        List<Ingredient> cheeses = ingredientRepository.findByNameContainingIgnoreCase("cheese");
        assertEquals(2, cheeses.size());

        // Search by grocery category
        List<Ingredient> produce = ingredientRepository.findByGroceryCategory(GroceryCategory.PRODUCE);
        assertEquals(1, produce.size());
        assertEquals("Romaine Lettuce", produce.get(0).getName());
    }

    @Test
    void createFullRecipeWithIngredients() {
        // Save ingredients first
        Ingredient beef = new Ingredient();
        beef.setName("Ground Beef");
        beef.setGroceryCategory(GroceryCategory.MEAT);
        beef.setStorageCategory(StorageCategory.REFRIGERATED);
        beef = ingredientRepository.save(beef);

        Ingredient cheese = new Ingredient();
        cheese.setName("Cheddar Cheese");
        cheese.setGroceryCategory(GroceryCategory.DAIRY);
        cheese.setStorageCategory(StorageCategory.REFRIGERATED);
        cheese = ingredientRepository.save(cheese);

        Ingredient shells = new Ingredient();
        shells.setName("Taco Shells");
        shells.setGroceryCategory(GroceryCategory.ETHNIC);
        shells.setStorageCategory(StorageCategory.DRY);
        shells = ingredientRepository.save(shells);

        // Build recipe
        Recipe tacos = new Recipe();
        tacos.setName("Beef Tacos");
        tacos.setInstructions("Brown the beef with taco seasoning. Warm shells. Assemble with cheese.");
        tacos.setIngredients(List.of(
                new RecipeIngredient(beef.getId(), beef.getName(),
                        new Measurement(new BigDecimal("1"), MeasurementUnit.LBS)),
                new RecipeIngredient(cheese.getId(), cheese.getName(),
                        new Measurement(new BigDecimal("2"), MeasurementUnit.CUP)),
                new RecipeIngredient(shells.getId(), shells.getName(),
                        new Measurement(new BigDecimal("8"), MeasurementUnit.PIECE))
        ));

        Recipe saved = recipeRepository.save(tacos);
        assertNotNull(saved.getId());

        // Retrieve and verify full structure
        Recipe found = recipeRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Beef Tacos", found.getName());
        assertEquals(3, found.getIngredients().size());

        RecipeIngredient beefIngredient = found.getIngredients().stream()
                .filter(ri -> ri.getIngredientName().equals("Ground Beef"))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("1"), beefIngredient.getMeasurement().getQuantity());
        assertEquals(MeasurementUnit.LBS, beefIngredient.getMeasurement().getUnit());
    }

    @Test
    void recipeSearchByName() {
        Recipe tacos = new Recipe();
        tacos.setName("Beef Tacos");
        tacos.setInstructions("Make tacos.");

        Recipe enchiladas = new Recipe();
        enchiladas.setName("Chicken Enchiladas");
        enchiladas.setInstructions("Make enchiladas.");

        Recipe pasta = new Recipe();
        pasta.setName("Spaghetti Bolognese");
        pasta.setInstructions("Make pasta.");

        recipeRepository.saveAll(List.of(tacos, enchiladas, pasta));

        List<Recipe> results = recipeRepository.findByNameContainingIgnoreCase("taco");
        assertEquals(1, results.size());
        assertEquals("Beef Tacos", results.get(0).getName());

        // Case-insensitive
        List<Recipe> results2 = recipeRepository.findByNameContainingIgnoreCase("CHICKEN");
        assertEquals(1, results2.size());
    }

    @Test
    void referenceEndpointsReturnEnums() throws Exception {
        mockMvc.perform(get("/api/v1/reference/measurement-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("CUP")))
                .andExpect(jsonPath("$", hasItem("LBS")));

        mockMvc.perform(get("/api/v1/reference/grocery-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("PRODUCE")))
                .andExpect(jsonPath("$", hasItem("MEAT")));

        mockMvc.perform(get("/api/v1/reference/storage-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("FROZEN")))
                .andExpect(jsonPath("$", hasItem("DRY")));
    }

    @Test
    void actuatorHealthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void recipeWithNestedIngredientsSurvivesMongoRoundTrip() {
        // Verify the full nested document structure persists and deserializes correctly
        Recipe recipe = new Recipe();
        recipe.setName("Grilled Cheese");
        recipe.setInstructions("Butter bread, add cheese, grill.");
        recipe.setIngredients(List.of(
                new RecipeIngredient("i1", "Bread",
                        new Measurement(new BigDecimal("2"), MeasurementUnit.PIECE)),
                new RecipeIngredient("i2", "Butter",
                        new Measurement(new BigDecimal("1"), MeasurementUnit.TBSP)),
                new RecipeIngredient("i3", "American Cheese",
                        new Measurement(new BigDecimal("2"), MeasurementUnit.PIECE))
        ));
        Recipe saved = recipeRepository.save(recipe);

        // Fetch fresh from DB
        Recipe found = recipeRepository.findById(saved.getId()).orElseThrow();

        assertEquals("Grilled Cheese", found.getName());
        assertEquals(3, found.getIngredients().size());

        RecipeIngredient bread = found.getIngredients().get(0);
        assertEquals("Bread", bread.getIngredientName());
        assertEquals("i1", bread.getIngredientId());
        assertEquals(0, new BigDecimal("2").compareTo(bread.getMeasurement().getQuantity()));
        assertEquals(MeasurementUnit.PIECE, bread.getMeasurement().getUnit());

        RecipeIngredient butter = found.getIngredients().get(1);
        assertEquals("Butter", butter.getIngredientName());
        assertEquals(MeasurementUnit.TBSP, butter.getMeasurement().getUnit());
    }
}
