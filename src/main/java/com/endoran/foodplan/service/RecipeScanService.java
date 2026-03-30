package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ImportedIngredientPreview;
import com.endoran.foodplan.dto.ImportedRecipePreview;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.Ingredient;
import com.endoran.foodplan.model.StorageCategory;
import com.endoran.foodplan.repository.IngredientRepository;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecipeScanService {

    private static final Pattern SERVINGS_PATTERN = Pattern.compile(
            "(?:serves?|servings?|yield|makes?)\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:time|prep|cook)\\s*:?\\s*(\\d+)\\s*(?:min|hour|hr)", Pattern.CASE_INSENSITIVE);

    private final RecipeImportService recipeImportService;
    private final IngredientRepository ingredientRepository;

    public RecipeScanService(RecipeImportService recipeImportService,
                             IngredientRepository ingredientRepository) {
        this.recipeImportService = recipeImportService;
        this.ingredientRepository = ingredientRepository;
    }

    public ImportedRecipePreview scanFile(String orgId, MultipartFile file) {
        String text = performOcr(file);
        return parseScannedText(orgId, text);
    }

    String performOcr(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.equals("application/pdf")) {
            return ocrPdf(file);
        }
        return ocrImage(file);
    }

    private String ocrImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new RecipeImportException("Could not read image file — supported formats: JPG, PNG, TIFF, BMP");
            }
            return ocrBufferedImage(image);
        } catch (IOException e) {
            throw new RecipeImportException("Failed to read image: " + e.getMessage(), e);
        }
    }

    private String ocrPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            StringBuilder allText = new StringBuilder();

            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                String pageText = ocrBufferedImage(image);
                if (!pageText.isBlank()) {
                    allText.append(pageText).append("\n");
                }
            }

            if (allText.isEmpty()) {
                throw new RecipeImportException("No text could be extracted from PDF");
            }
            return allText.toString();
        } catch (IOException e) {
            throw new RecipeImportException("Failed to read PDF: " + e.getMessage(), e);
        }
    }

    private String ocrBufferedImage(BufferedImage image) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setLanguage("eng");
            String datapath = System.getenv("TESSDATA_PREFIX");
            if (datapath != null && !datapath.isEmpty()) {
                tesseract.setDatapath(datapath);
            }
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            throw new RecipeImportException("OCR failed: " + e.getMessage(), e);
        }
    }

    ImportedRecipePreview parseScannedText(String orgId, String text) {
        String[] lines = text.split("\\n");
        List<String> nonEmpty = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) nonEmpty.add(trimmed);
        }

        if (nonEmpty.isEmpty()) {
            throw new RecipeImportException("No text could be extracted from the image");
        }

        // Title = first non-empty line
        String title = nonEmpty.get(0);

        // Extract servings
        int servings = 1;
        Matcher servMatcher = SERVINGS_PATTERN.matcher(text);
        if (servMatcher.find()) {
            servings = Integer.parseInt(servMatcher.group(1));
        }

        // Split into ingredients section and instructions
        List<String> ingredientLines = new ArrayList<>();
        List<String> instructionLines = new ArrayList<>();
        boolean inInstructions = false;

        for (int i = 1; i < nonEmpty.size(); i++) {
            String line = nonEmpty.get(i);
            String lower = line.toLowerCase();

            // Detect section headers
            if (lower.matches("^(instructions?|directions?|method|steps?|preparation)\\s*:?\\s*$")) {
                inInstructions = true;
                continue;
            }
            if (lower.matches("^(ingredients?)\\s*:?\\s*$")) {
                inInstructions = false;
                continue;
            }

            // Skip servings/time lines
            if (SERVINGS_PATTERN.matcher(line).find() || TIME_PATTERN.matcher(line).find()) {
                continue;
            }

            if (inInstructions) {
                instructionLines.add(line);
            } else {
                ingredientLines.add(line);
            }
        }

        // If we never found an "Instructions" header, assume second half is instructions
        if (instructionLines.isEmpty() && ingredientLines.size() > 3) {
            int split = ingredientLines.size() / 2;
            instructionLines = new ArrayList<>(ingredientLines.subList(split, ingredientLines.size()));
            ingredientLines = new ArrayList<>(ingredientLines.subList(0, split));
        }

        // Parse ingredients
        List<ImportedIngredientPreview> ingredients = ingredientLines.stream()
                .map(recipeImportService::parseIngredientText)
                .toList();

        // Auto-create unknown ingredients
        autoCreateIngredients(orgId, ingredients);

        // Build instructions text
        StringBuilder instBuilder = new StringBuilder();
        for (int i = 0; i < instructionLines.size(); i++) {
            String line = instructionLines.get(i);
            if (!line.matches("^\\d+\\..*")) {
                instBuilder.append(i + 1).append(". ");
            }
            instBuilder.append(line).append("\n");
        }

        return new ImportedRecipePreview(title, instBuilder.toString().trim(), servings, ingredients, "scan");
    }

    private void autoCreateIngredients(String orgId, List<ImportedIngredientPreview> ingredients) {
        for (ImportedIngredientPreview ing : ingredients) {
            List<Ingredient> existing = ingredientRepository.findByOrgIdAndNameContainingIgnoreCase(
                    orgId, ing.name());
            if (existing.isEmpty()) {
                Ingredient newIng = new Ingredient();
                newIng.setOrgId(orgId);
                newIng.setName(ing.name());
                newIng.setStorageCategory(StorageCategory.DRY);
                newIng.setGroceryCategory(GroceryCategory.PRODUCE);
                newIng.setNeedsReview(true);
                ingredientRepository.save(newIng);
            }
        }
    }
}
