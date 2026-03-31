package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ImportedIngredientPreview;
import com.endoran.foodplan.dto.ImportedRecipePreview;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecipeScanService {

    private static final Pattern SERVINGS_PATTERN = Pattern.compile(
            "(?:serves?|servings?|yield|makes?)\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:time|prep|cook)\\s*:?\\s*(\\d+)\\s*(?:min|hour|hr)", Pattern.CASE_INSENSITIVE);
    // Sub-section headers: lines with a colon like "For the Marinade:", "Sauce:"
    // OR short all-letter lines (≤4 words, no digits) like "Soup", "Chicken Rub"
    private static final Pattern SUB_SECTION_WITH_COLON = Pattern.compile(
            "^(?:for\\s+)?(?:the\\s+)?[\\p{L} ]+:\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUB_SECTION_SHORT_LABEL = Pattern.compile(
            "^[\\p{L}][\\p{L} ]{0,30}$");

    private final RecipeImportService recipeImportService;

    public RecipeScanService(RecipeImportService recipeImportService) {
        this.recipeImportService = recipeImportService;
    }

    public ImportedRecipePreview scanFile(MultipartFile file) {
        String text = performOcr(file);
        return parseScannedText(text);
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

    ImportedRecipePreview parseScannedText(String text) {
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
        // Ingredients are stored as (section, line) pairs to preserve sub-recipe grouping
        List<Map.Entry<String, String>> ingredientEntries = new ArrayList<>();
        List<String> instructionLines = new ArrayList<>();
        boolean inInstructions = false;
        String currentSection = null;

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

            // Detect sub-recipe section headers (only in ingredients)
            if (!inInstructions) {
                // Lines with colon: "For the Marinade:", "Sauce:"
                if (SUB_SECTION_WITH_COLON.matcher(line).matches()) {
                    currentSection = cleanSectionName(line);
                    continue;
                }
                // Short all-letter lines (≤4 words) without digits: "Soup", "Chicken Rub"
                if (SUB_SECTION_SHORT_LABEL.matcher(line).matches()
                        && line.split("\\s+").length <= 4) {
                    currentSection = cleanSectionName(line);
                    continue;
                }
            }

            if (inInstructions) {
                // Filter OCR garbage: short non-numeric, non-header lines
                boolean isShortGarbage = line.length() <= 5
                        && !Character.isDigit(line.charAt(0))
                        && !line.toLowerCase().matches("notes?|tips?");
                if (isShortGarbage) {
                    continue;
                }
                instructionLines.add(line);
            } else {
                ingredientEntries.add(new AbstractMap.SimpleEntry<>(currentSection, line));
            }
        }

        // If we never found an "Instructions" header, assume second half is instructions
        if (instructionLines.isEmpty() && ingredientEntries.size() > 3) {
            int split = ingredientEntries.size() / 2;
            for (int i = split; i < ingredientEntries.size(); i++) {
                instructionLines.add(ingredientEntries.get(i).getValue());
            }
            ingredientEntries = new ArrayList<>(ingredientEntries.subList(0, split));
        }

        // Parse ingredients — strip leading OCR artifacts (bullets, slashes, dashes, etc.).
        // Trim whitespace, then strip any leading non-alphanumeric junk until we hit a
        // letter or digit (the start of a quantity or ingredient name).
        List<ImportedIngredientPreview> ingredients = ingredientEntries.stream()
                .filter(entry -> {
                    String cleaned = entry.getValue().trim().replaceFirst("^[^\\p{L}\\p{N}]+", "").trim();
                    return !cleaned.isEmpty();
                })
                .map(entry -> {
                    String section = entry.getKey();
                    String line = entry.getValue().trim().replaceFirst("^[^\\p{L}\\p{N}]+", "").trim();
                    ImportedIngredientPreview parsed = recipeImportService.parseIngredientText(line);
                    return new ImportedIngredientPreview(section, parsed.name(), parsed.quantity(), parsed.unit(), parsed.rawText());
                })
                .toList();

        // Build instructions — merge continuation lines from multi-line OCR steps.
        //
        // Two modes depending on how the OCR captured the numbering:
        //   "inline"   — number + text on same line:  "1. Heat a pan on medium."
        //   "orphaned" — number alone on its own line: "1."  then "Heat a pan..."
        //
        // Inline mode: uppercase after "N." = new step; lowercase = continuation.
        // Orphaned mode: skip bare number lines; each uppercase-starting line = new step;
        //   lowercase-starting lines = continuation of previous step.
        //
        // Notes/Tips sections and bullet lines are handled in both modes.
        List<String> mergedInstructions = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        java.util.regex.Pattern inlineStepPattern = java.util.regex.Pattern.compile("^\\d+\\.\\s+([A-Z].+)");
        java.util.regex.Pattern bareNumberPattern = java.util.regex.Pattern.compile("^\\d+\\.\\s*$");
        java.util.regex.Pattern numPrefixPattern = java.util.regex.Pattern.compile("^\\d+\\.\\s*(.*)");
        java.util.regex.Pattern notesHeaderPattern = java.util.regex.Pattern.compile(
                "^(?:\\d+\\.\\s*)?(?i)(notes?|tips?|variations?)\\s*$");
        java.util.regex.Pattern bulletPattern = java.util.regex.Pattern.compile(
                "^(?:\\d+\\.\\s*)?[+*\\-•]\\s*(.+)");

        // Determine mode: count inline vs orphaned numbered lines
        long inlineCount = instructionLines.stream()
                .filter(l -> inlineStepPattern.matcher(l).matches())
                .count();
        long orphanedCount = instructionLines.stream()
                .filter(l -> bareNumberPattern.matcher(l).matches())
                .count();
        boolean useOrphanedMode = orphanedCount > inlineCount;
        boolean inNotes = false;

        for (String line : instructionLines) {
            // Notes/tips section header
            if (notesHeaderPattern.matcher(line).matches()) {
                inNotes = true;
                continue;
            }

            // Bullet lines (+ / * / -)
            java.util.regex.Matcher bulletMatcher = bulletPattern.matcher(line);
            if (bulletMatcher.matches()) {
                if (inNotes) {
                    notes.add(bulletMatcher.group(1));
                } else {
                    mergedInstructions.add(bulletMatcher.group(1));
                }
                continue;
            }

            if (inNotes) {
                // Non-bullet continuation in notes section
                if (!notes.isEmpty()) {
                    int last = notes.size() - 1;
                    java.util.regex.Matcher numMatcher = numPrefixPattern.matcher(line);
                    String content = numMatcher.matches() ? numMatcher.group(1) : line;
                    if (!content.isEmpty()) {
                        notes.set(last, notes.get(last) + " " + content);
                    }
                }
                continue;
            }

            // Skip bare number lines ("10.", "11.") in both modes
            if (bareNumberPattern.matcher(line).matches()) {
                continue;
            }

            if (useOrphanedMode) {
                // Orphaned mode: each uppercase-starting sentence is a new step
                // Strip any leading ". " from OCR artifacts (". Once roasted...")
                String cleaned = line.replaceFirst("^\\d*\\.\\s*", "").trim();
                if (cleaned.isEmpty()) continue;

                if (Character.isUpperCase(cleaned.charAt(0))) {
                    mergedInstructions.add(cleaned);
                } else if (!mergedInstructions.isEmpty()) {
                    int last = mergedInstructions.size() - 1;
                    mergedInstructions.set(last, mergedInstructions.get(last) + " " + cleaned);
                } else {
                    mergedInstructions.add(cleaned);
                }
            } else {
                // Inline mode: "N. Uppercase" = new step, lowercase = continuation
                java.util.regex.Matcher stepMatcher = inlineStepPattern.matcher(line);
                if (stepMatcher.matches()) {
                    mergedInstructions.add(stepMatcher.group(1));
                } else if (!mergedInstructions.isEmpty()) {
                    java.util.regex.Matcher numMatcher = numPrefixPattern.matcher(line);
                    String content = numMatcher.matches() ? numMatcher.group(1) : line;
                    int last = mergedInstructions.size() - 1;
                    mergedInstructions.set(last, mergedInstructions.get(last) + " " + content);
                } else {
                    mergedInstructions.add(line);
                }
            }
        }

        StringBuilder instBuilder = new StringBuilder();
        for (int i = 0; i < mergedInstructions.size(); i++) {
            instBuilder.append(i + 1).append(". ").append(mergedInstructions.get(i)).append("\n");
        }
        if (!notes.isEmpty()) {
            instBuilder.append("\nNotes:\n");
            for (String note : notes) {
                instBuilder.append("- ").append(note).append("\n");
            }
        }

        return new ImportedRecipePreview(title, instBuilder.toString().trim(), servings, ingredients, "scan");
    }

    private static String cleanSectionName(String header) {
        return header
                .replaceFirst(":$", "")
                .replaceFirst("(?i)^for\\s+the\\s+", "")
                .replaceFirst("(?i)^for\\s+", "")
                .trim();
    }

}
