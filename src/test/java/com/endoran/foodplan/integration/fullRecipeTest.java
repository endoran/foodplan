//package com.endoran.services.foodplan.integration;
//
//import com.endoran.services.foodplan.model.GroceryCategory;
//import com.endoran.services.foodplan.model.Ingredient;
//import com.endoran.services.foodplan.model.Measurement;
//import com.endoran.services.foodplan.model.MeasurementCategory;
//import com.endoran.services.foodplan.model.Recipe;
//import com.endoran.services.foodplan.model.StorageCategory;
//import com.endoran.services.foodplan.repository.IngredientRepository;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RunWith(SpringRunner.class)
//@DataJpaTest
//public class fullRecipeTest {
//
////    @Autowired
////    private TestEntityManager entityManager;
//
//    @Autowired
//    private IngredientRepository ingredientRepository;
////
////    @Autowired
////    private RecipeRepository recipeRepository;
//
//    @Test
//    public void doStuff() {
//        // Hahahaha
//    }
//
//    @Test
//    public void testCreateRecipe() {
//        Ingredient cheese;
//        Ingredient lettuce;
//        Measurement cheeseMeasurement;
//        Measurement lettuceMeasurement;
//        Recipe tacos;
//
//        int cheeseQuantity = 2;
//        int lettuceQuantity = 1;
//        MeasurementCategory cheeseMeasurementCategory = MeasurementCategory.cup;
//        MeasurementCategory lettuceMeasurementCategory = MeasurementCategory.unit;
//
//        cheese = new Ingredient();
//        cheese.setId("0");
//        cheese.setGroceryCategory(GroceryCategory.DAIRY);
//        cheese.setName("cheese");
//        cheese.setStorageCategory(StorageCategory.REFRIGERATED);
//
//        lettuce = new Ingredient();
//        lettuce.setId("1");
//        lettuce.setGroceryCategory(GroceryCategory.PRODUCE);
//        lettuce.setName("lettuce");
//        lettuce.setStorageCategory(StorageCategory.REFRIGERATED);
//
//        lettuceMeasurement = new Measurement(lettuceQuantity, lettuceMeasurementCategory);
//        cheeseMeasurement = new Measurement(cheeseQuantity, cheeseMeasurementCategory);
//
//        ingredientRepository.save(cheese);
//        ingredientRepository.save(lettuce);
//
//        tacos = new Recipe();
//        Map<Measurement, Ingredient> ingredients = new HashMap<>();
//        ingredients.put(cheeseMeasurement, cheese);
//        ingredients.put(lettuceMeasurement, lettuce);
//        tacos.setIngredients(ingredients);
//        tacos.setName("tacos");
//
//        recipeRepository.save(tacos);
//        System.out.println(recipeRepository.toString());
//    }
//
//}
