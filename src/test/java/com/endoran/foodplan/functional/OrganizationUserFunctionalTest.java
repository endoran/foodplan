package com.endoran.foodplan.functional;

import com.endoran.foodplan.model.*;
import com.endoran.foodplan.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationUserFunctionalTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
    }

    @Test
    void createAndRetrieveOrganization() {
        Organization org = new Organization();
        org.setName("Gomez Family");

        Organization saved = organizationRepository.save(org);
        assertNotNull(saved.getId());

        Organization found = organizationRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("Gomez Family", found.getName());
        assertEquals("America/Chicago", found.getSettings().getTimezone());
        assertEquals(4, found.getSettings().getDefaultServings());
    }

    @Test
    void organizationWithCustomSettings() {
        Organization org = new Organization();
        org.setName("Camp Wilderness");
        org.setSettings(new OrgSettings("America/Denver", 50));

        Organization saved = organizationRepository.save(org);
        Organization found = organizationRepository.findById(saved.getId()).orElseThrow();

        assertEquals("Camp Wilderness", found.getName());
        assertEquals("America/Denver", found.getSettings().getTimezone());
        assertEquals(50, found.getSettings().getDefaultServings());
    }

    @Test
    void findOrganizationByName() {
        Organization org1 = new Organization();
        org1.setName("Grace Church");
        organizationRepository.save(org1);

        Organization org2 = new Organization();
        org2.setName("Camp Sunrise");
        organizationRepository.save(org2);

        List<Organization> results = organizationRepository.findByNameContainingIgnoreCase("church");
        assertEquals(1, results.size());
        assertEquals("Grace Church", results.get(0).getName());
    }

    @Test
    void createAndRetrieveUser() {
        Organization org = new Organization();
        org.setName("Test Org");
        org = organizationRepository.save(org);

        User user = new User();
        user.setEmail("pete@example.com");
        user.setPasswordHash("$2a$10$somebcrypthash");
        user.setOrgId(org.getId());
        user.setRole(UserRole.OWNER);

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());

        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals("pete@example.com", found.getEmail());
        assertEquals(org.getId(), found.getOrgId());
        assertEquals(UserRole.OWNER, found.getRole());
        assertEquals("$2a$10$somebcrypthash", found.getPasswordHash());
    }

    @Test
    void findUserByEmail() {
        Organization org = new Organization();
        org.setName("Test Org");
        org = organizationRepository.save(org);

        User user = new User();
        user.setEmail("find.me@example.com");
        user.setOrgId(org.getId());
        user.setRole(UserRole.MEMBER);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("find.me@example.com");
        assertTrue(found.isPresent());
        assertEquals("find.me@example.com", found.get().getEmail());

        Optional<User> notFound = userRepository.findByEmail("nope@example.com");
        assertFalse(notFound.isPresent());
    }

    @Test
    void findUsersByOrgId() {
        Organization org = new Organization();
        org.setName("Team Org");
        org = organizationRepository.save(org);

        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setOrgId(org.getId());
        user1.setRole(UserRole.OWNER);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setOrgId(org.getId());
        user2.setRole(UserRole.MEMBER);

        userRepository.saveAll(List.of(user1, user2));

        List<User> orgUsers = userRepository.findByOrgId(org.getId());
        assertEquals(2, orgUsers.size());
    }

    @Test
    void findUserByOauthProviderAndOauthId() {
        Organization org = new Organization();
        org.setName("OAuth Org");
        org = organizationRepository.save(org);

        User user = new User();
        user.setEmail("oauth@example.com");
        user.setOauthProvider("google");
        user.setOauthId("google-uid-12345");
        user.setOrgId(org.getId());
        user.setRole(UserRole.MEMBER);
        userRepository.save(user);

        Optional<User> found = userRepository.findByOauthProviderAndOauthId("google", "google-uid-12345");
        assertTrue(found.isPresent());
        assertEquals("oauth@example.com", found.get().getEmail());

        Optional<User> wrongProvider = userRepository.findByOauthProviderAndOauthId("github", "google-uid-12345");
        assertFalse(wrongProvider.isPresent());
    }

    @Test
    void userEmailUniquenessEnforced() {
        Organization org = new Organization();
        org.setName("Unique Org");
        org = organizationRepository.save(org);

        User user1 = new User();
        user1.setEmail("dupe@example.com");
        user1.setOrgId(org.getId());
        user1.setRole(UserRole.OWNER);
        userRepository.save(user1);

        User user2 = new User();
        user2.setEmail("dupe@example.com");
        user2.setOrgId(org.getId());
        user2.setRole(UserRole.MEMBER);

        assertThrows(DuplicateKeyException.class, () -> userRepository.save(user2));
    }

    @Test
    void orgScopedRecipeQueries() {
        Organization org1 = new Organization();
        org1.setName("Org One");
        org1 = organizationRepository.save(org1);

        Organization org2 = new Organization();
        org2.setName("Org Two");
        org2 = organizationRepository.save(org2);

        Recipe tacos = new Recipe();
        tacos.setName("Beef Tacos");
        tacos.setOrgId(org1.getId());
        recipeRepository.save(tacos);

        Recipe enchiladas = new Recipe();
        enchiladas.setName("Chicken Enchiladas");
        enchiladas.setOrgId(org1.getId());
        recipeRepository.save(enchiladas);

        Recipe pasta = new Recipe();
        pasta.setName("Taco Pasta");
        pasta.setOrgId(org2.getId());
        recipeRepository.save(pasta);

        // findByOrgId
        List<Recipe> org1Recipes = recipeRepository.findByOrgId(org1.getId());
        assertEquals(2, org1Recipes.size());

        List<Recipe> org2Recipes = recipeRepository.findByOrgId(org2.getId());
        assertEquals(1, org2Recipes.size());

        // findByOrgIdAndNameContainingIgnoreCase — "taco" exists in both orgs
        List<Recipe> org1Tacos = recipeRepository.findByOrgIdAndNameContainingIgnoreCase(org1.getId(), "taco");
        assertEquals(1, org1Tacos.size());
        assertEquals("Beef Tacos", org1Tacos.get(0).getName());

        List<Recipe> org2Tacos = recipeRepository.findByOrgIdAndNameContainingIgnoreCase(org2.getId(), "taco");
        assertEquals(1, org2Tacos.size());
        assertEquals("Taco Pasta", org2Tacos.get(0).getName());
    }

    @Test
    void orgScopedIngredientQueries() {
        Organization org1 = new Organization();
        org1.setName("Org A");
        org1 = organizationRepository.save(org1);

        Organization org2 = new Organization();
        org2.setName("Org B");
        org2 = organizationRepository.save(org2);

        Ingredient cheese1 = new Ingredient();
        cheese1.setName("Cheddar Cheese");
        cheese1.setOrgId(org1.getId());
        cheese1.setGroceryCategory(GroceryCategory.DAIRY);
        cheese1.setStorageCategory(StorageCategory.REFRIGERATED);
        ingredientRepository.save(cheese1);

        Ingredient cheese2 = new Ingredient();
        cheese2.setName("Swiss Cheese");
        cheese2.setOrgId(org2.getId());
        cheese2.setGroceryCategory(GroceryCategory.DAIRY);
        cheese2.setStorageCategory(StorageCategory.REFRIGERATED);
        ingredientRepository.save(cheese2);

        Ingredient lettuce = new Ingredient();
        lettuce.setName("Romaine Lettuce");
        lettuce.setOrgId(org1.getId());
        lettuce.setGroceryCategory(GroceryCategory.PRODUCE);
        lettuce.setStorageCategory(StorageCategory.FRESH);
        ingredientRepository.save(lettuce);

        // findByOrgId
        assertEquals(2, ingredientRepository.findByOrgId(org1.getId()).size());
        assertEquals(1, ingredientRepository.findByOrgId(org2.getId()).size());

        // findByOrgIdAndNameContainingIgnoreCase
        List<Ingredient> org1Cheese = ingredientRepository.findByOrgIdAndNameContainingIgnoreCase(org1.getId(), "cheese");
        assertEquals(1, org1Cheese.size());
        assertEquals("Cheddar Cheese", org1Cheese.get(0).getName());

        // findByOrgIdAndGroceryCategory
        List<Ingredient> org1Dairy = ingredientRepository.findByOrgIdAndGroceryCategory(org1.getId(), GroceryCategory.DAIRY);
        assertEquals(1, org1Dairy.size());

        List<Ingredient> org2Dairy = ingredientRepository.findByOrgIdAndGroceryCategory(org2.getId(), GroceryCategory.DAIRY);
        assertEquals(1, org2Dairy.size());
        assertEquals("Swiss Cheese", org2Dairy.get(0).getName());
    }
}
