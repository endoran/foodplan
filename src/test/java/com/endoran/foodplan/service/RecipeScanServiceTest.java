package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ImportedIngredientPreview;
import com.endoran.foodplan.dto.ImportedRecipePreview;
import org.junit.jupiter.api.Test;

class RecipeScanServiceTest {

    private static final String OCR_TEXT = """
            Butter Chicken Soup - ~1 Gal.

            Ingredients

            Soup

            4lbs / 1 Whole Young Chicken
            1/4 Gallon Chicken Stock

            3.5 Cups Water

            3 Fresh Tomatoes, Cubed

            2 Cans Diced Tomatoes

            3 Large Yellow Onions, Rough Chopped
            1 Tbsp Canola Oil

            1/2 Tbsp Red Pepper Flakes

            1/2 Tposp Cumin

            1 1/2 Tbsp Salt, divided

            1/2 Bulb Fresh Garlic, Minced

            2 Tbsp Garam Masala

            1/2 Cup Butter

            1 1/2 Cups Cashew Nuts

            2 Cups Cilantro, Finely Chopped
            2 Cups Heavy Cream

            Chicken Rub

            2 Tbsp Garam Masala

            1/2 Tbsp Cumin

            1/2 Tbsp Salt

            1/2 Tbsp Red Pepper Flakes
            1 Tbsp Garlic Powder

            Instructions

            RON -

            OND

            10.
            11.

            Heat a medium frying pan for 2 minutes on medium heat.

            Add canola oil, chopped onions, and 1 tsp salt to the pan.

            Caramelize onions until golden brown, stirring occasionally.

            Just before onions are fully caramelized, add butter, red pepper flakes, cumin, and garam
            masala; stir in and cook for another 4 minutes.

            Dump frying pan contents into a large stock pot, at least 6 quarts.

            Add remaining salt, chicken stock, water, and tomatoes (fresh and canned).

            Heat soup on low to 180 degrees, hold at this temperature.

            Blend cashews until fine, then add 3 cups soup to blender and finish blend until very
            smooth and creamy. Add to soup.

            Add 1 1/2 cups cilantro to soup.

            Simmer for 5 hours at precisely 180 degrees while preparing the chicken.

            Spatchcock chicken, cutting along each side of the spine, from rear-end to neck, neatly
            removing approximately a 1-inch strip from the back of the chicken. Flip over and press
            hard between the breasts until the breastbone cracks and the breasts lay flat and loose
            from each other.
            12.

            13.
            14.

            Clip and dispose of excess fat on neck and rear, as well as any organs left inside the
            chicken.

            Lightly rub oil around every surface of the chicken.

            Mix rub ingredients, rub all around both chickens thoroughly, making sure to coat every
            possible surface, under wings and legs, as well as inside where the spine was removed. If
            possible, rub under the skin for more flavor.

            Roast chicken in oven, barbecue, or smoker at 375 degrees until largest breast
            temperature reaches 160 degrees at its center.

            . Once roasted, remove chicken from heat, wrap immediately in two layers of plastic wrap,
            then a layer of foil. Wrap again in a large towel, and place in a cooler to rest.

            . After simmer time is complete, blend the soup entirely until smooth and creamy.

            Put soup back over low heat; bring to 180 degrees.
            Unwrap, remove the skin, and extract the meat from the roasted chicken.

            . Cut meat into cubes, about 3/4", (or shred if preferred) and add to soup.
            . Add remaining cilantro.
            . Add heavy cream, cook an additional 5 minutes at 180 degrees.

            Notes

            * This recipe achieves approximately a 2 for spiciness, on the Thai scale; increase or decrease
            as desired.

            * Chicken may be cooked in the soup, if desired, in which case various other sources for
            chicken may be used, including whole frozen chicken breasts or thighs or both.

            If using frozen chicken and cooking it in the soup, add them when 1 hour remains for
            cooking, making sure to fish it out before blending. Then resume at step 20.

            + Another option for alternate chicken could include pre-cooked rotisserie chicken from
            Costco.
            """;

    @Test
    void parseButterChickenSoup() {
        RecipeImportService importService = new RecipeImportService();
        RecipeScanService scanService = new RecipeScanService(importService);
        ImportedRecipePreview preview = scanService.parseScannedText(OCR_TEXT);

        System.out.println("Title: " + preview.name());
        System.out.println("Servings: " + preview.baseServings());
        System.out.println("\nIngredients (" + preview.ingredients().size() + "):");
        for (int i = 0; i < preview.ingredients().size(); i++) {
            ImportedIngredientPreview ing = preview.ingredients().get(i);
            System.out.printf("  %2d. qty=%-8s unit=%-6s name=%-30s raw=%s%n",
                    i + 1, ing.quantity(), ing.unit(), ing.name(), ing.rawText());
        }
        System.out.println("\nInstructions:\n" + preview.instructions());
    }
}
