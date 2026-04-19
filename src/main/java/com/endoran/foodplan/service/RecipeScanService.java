package com.endoran.foodplan.service;

import com.endoran.foodplan.dto.ImportedIngredientPreview;
import com.endoran.foodplan.dto.ImportedRecipePreview;
import com.endoran.foodplan.dto.ScanResult;
import com.endoran.foodplan.model.ScanSession;
import com.endoran.foodplan.repository.ScanSessionRepository;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class RecipeScanService {

    private static final Logger log = LoggerFactory.getLogger(RecipeScanService.class);

    private static final Pattern SERVINGS_PATTERN = Pattern.compile(
            "(?:serves?|servings?|yield|makes?)\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?:time|prep|cook)\\s*:?\\s*(\\d+)\\s*(?:min|hour|hr)", Pattern.CASE_INSENSITIVE);
    private static final Pattern METADATA_PATTERN = Pattern.compile(
            "^(?:course|cuisine|keyword|category|author|calories|total\\s*time|prep\\s*time|cook\\s*time|" +
            "refrigerat\\w*|resting|cooling|nutrition|yield)\\b.*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SUB_SECTION_WITH_COLON = Pattern.compile(
            "^(?:for\\s+)?(?:the\\s+)?[\\p{L} ]+:\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUB_SECTION_SHORT_LABEL = Pattern.compile(
            "^[\\p{L}][\\p{L} ]{0,30}$");

    private final RecipeImportService recipeImportService;
    private final OllamaRecipeExtractor ollamaExtractor;
    private final ScanSessionRepository scanSessionRepository;

    public RecipeScanService(RecipeImportService recipeImportService,
                             OllamaRecipeExtractor ollamaExtractor,
                             ScanSessionRepository scanSessionRepository) {
        this.recipeImportService = recipeImportService;
        this.ollamaExtractor = ollamaExtractor;
        this.scanSessionRepository = scanSessionRepository;
    }


    private static boolean isHeic(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && (ct.equals("image/heic") || ct.equals("image/heif"))) return true;
        String name = file.getOriginalFilename();
        return name != null && (name.toLowerCase().endsWith(".heic") || name.toLowerCase().endsWith(".heif"));
    }

    private byte[] convertHeicToJpeg(MultipartFile file) {
        Path heicPath = null;
        Path jpegPath = null;
        try {
            heicPath = Files.createTempFile("scan-", ".heic");
            jpegPath = Path.of(heicPath.toString().replace(".heic", ".jpg"));
            file.transferTo(heicPath);

            ProcessBuilder pb = new ProcessBuilder("heif-convert", heicPath.toString(), jpegPath.toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            int exit = proc.waitFor();
            if (exit != 0) {
                throw new RecipeImportException("HEIC conversion failed (exit " + exit + "): " + output);
            }

            log.info("Converted HEIC to JPEG ({} KB -> {} KB)",
                    Files.size(heicPath) / 1024, Files.size(jpegPath) / 1024);
            return Files.readAllBytes(jpegPath);
        } catch (RecipeImportException e) {
            throw e;
        } catch (Exception e) {
            throw new RecipeImportException("Failed to convert HEIC: " + e.getMessage(), e);
        } finally {
            try { if (heicPath != null) Files.deleteIfExists(heicPath); } catch (Exception ignored) {}
            try { if (jpegPath != null) Files.deleteIfExists(jpegPath); } catch (Exception ignored) {}
        }
    }

    public ScanResult scanFile(MultipartFile file, String orgId) {
        // Convert HEIC/HEIF to JPEG before any processing
        if (isHeic(file)) {
            byte[] jpegBytes = convertHeicToJpeg(file);
            file = new SimpleMultipartFile(file.getOriginalFilename() + ".jpg", "image/jpeg", jpegBytes);
        }

        // Capture image bytes for training pair storage
        byte[] imageBytes;
        String imageContentType = file.getContentType();
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            throw new RecipeImportException("Failed to read file: " + e.getMessage(), e);
        }

        // Auto-rotate landscape images for vision model (recipe cards are almost always portrait)
        byte[] visionBytes = autoRotateForVision(imageBytes, imageContentType);

        List<ImportedRecipePreview> recipes;
        String tier;

        // Tier 1 & 2: Try LLM extraction if Ollama is available
        if (ollamaExtractor.isAvailable()) {
            // Tier 1: Vision — send photo directly to vision model
            try {
                List<ImportedRecipePreview> vision = ollamaExtractor.extractFromImage(visionBytes, imageContentType);
                if (!vision.isEmpty()) {
                    log.info("Extracted {} recipe(s) via vision LLM", vision.size());
                    recipes = vision;
                    tier = "VISION";
                    return saveScanSession(orgId, imageBytes, imageContentType, recipes, tier);
                }
            } catch (Exception e) {
                log.warn("Vision extraction attempt failed: {}", e.getMessage());
            }

            // Tier 2: Text LLM — OCR first, then send text to LLM
            String ocrText = performOcr(file);
            List<ImportedRecipePreview> textLlm = ollamaExtractor.extractFromText(ocrText);
            if (!textLlm.isEmpty()) {
                log.info("Extracted {} recipe(s) via text LLM (OCR + LLM)", textLlm.size());
                recipes = textLlm;
                tier = "TEXT_LLM";
                return saveScanSession(orgId, imageBytes, imageContentType, recipes, tier);
            }

            // Tier 3: Regex fallback — reuse OCR text already captured
            log.info("Recipe extracted via regex fallback");
            recipes = List.of(parseScannedText(ocrText));
            tier = "REGEX";
            return saveScanSession(orgId, imageBytes, imageContentType, recipes, tier);
        }

        // Ollama not available — go straight to regex fallback
        log.info("Ollama unavailable, using regex fallback");
        String text = performOcr(file);
        recipes = List.of(parseScannedText(text));
        tier = "REGEX";
        return saveScanSession(orgId, imageBytes, imageContentType, recipes, tier);
    }

    private ScanResult saveScanSession(String orgId, byte[] imageBytes, String imageContentType,
                                        List<ImportedRecipePreview> recipes, String tier) {
        ScanSession session = new ScanSession();
        session.setOrgId(orgId);
        session.setImageData(imageBytes);
        session.setImageContentType(imageContentType);
        session.setModelOutput(recipes);
        session.setExtractionTier(tier);
        session = scanSessionRepository.save(session);
        log.info("Saved scan session {} (tier={}, recipes={})", session.getId(), tier, recipes.size());
        return new ScanResult(session.getId(), recipes);
    }

    private byte[] autoRotateForVision(byte[] imageBytes, String contentType) {
        try {
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            if (img == null) return imageBytes;

            boolean modified = false;

            // Use Tesseract OSD to detect text orientation
            int osdRotation = detectRotationDegrees(img);
            if (osdRotation != 0) {
                log.info("OSD detected {}° rotation for vision — correcting", osdRotation);
                img = rotateImage(img, Math.toRadians(osdRotation));
                modified = true;
            }

            // Fallback: rotate landscape images (only if OSD didn't already rotate)
            if (!modified && img.getWidth() > img.getHeight() * 1.2) {
                log.info("Auto-rotating landscape image {}x{} -> portrait for vision", img.getWidth(), img.getHeight());
                img = rotateImage(img, Math.PI / 2);
                modified = true;
            }

            // Downscale for vision model — Qwen2.5-VL works best at ~1568px max dimension
            int maxDim = Math.max(img.getWidth(), img.getHeight());
            if (maxDim > 1568) {
                img = downscaleForVision(img, 1568);
                modified = true;
            }

            if (!modified) return imageBytes;

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            String format = contentType != null && contentType.contains("png") ? "png" : "jpg";
            ImageIO.write(img, format, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Auto-rotation failed, using original: {}", e.getMessage());
            return imageBytes;
        }
    }

    private BufferedImage downscaleForVision(BufferedImage img, int maxDim) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w <= maxDim && h <= maxDim) return img;

        double scale = (double) maxDim / Math.max(w, h);
        int nw = (int) (w * scale);
        int nh = (int) (h * scale);
        log.info("Downscaling image {}x{} -> {}x{} for vision model", w, h, nw, nh);
        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();
        return scaled;
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

            // Apply EXIF orientation — iPhone photos are stored rotated with an EXIF
            // tag that viewers apply automatically, but ImageIO.read() ignores it.
            image = applyExifOrientation(file, image);

            return ocrBufferedImage(image);
        } catch (IOException e) {
            throw new RecipeImportException("Failed to read image: " + e.getMessage(), e);
        }
    }

    private BufferedImage applyExifOrientation(MultipartFile file, BufferedImage image) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());
            ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exif == null || !exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return image;
            }

            int orientation = exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            log.info("EXIF orientation tag: {}", orientation);

            return switch (orientation) {
                case 3 -> rotateImage(image, Math.PI);           // 180
                case 6 -> rotateImage(image, Math.PI / 2);       // 90 CW
                case 8 -> rotateImage(image, -Math.PI / 2);      // 90 CCW
                default -> image;                                 // 1 = normal
            };
        } catch (Exception e) {
            log.warn("Could not read EXIF orientation: {}", e.getMessage());
            return image;
        }
    }

    private BufferedImage rotateImage(BufferedImage src, double radians) {
        int w = src.getWidth();
        int h = src.getHeight();
        boolean is90 = Math.abs(Math.abs(radians) - Math.PI / 2) < 0.01;
        int newW = is90 ? h : w;
        int newH = is90 ? w : h;

        BufferedImage rotated = new BufferedImage(newW, newH, src.getType());
        java.awt.Graphics2D g = rotated.createGraphics();
        g.translate(newW / 2.0, newH / 2.0);
        g.rotate(radians);
        g.translate(-w / 2.0, -h / 2.0);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rotated;
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
            BufferedImage processed = preprocessForOcr(image);
            processed = detectAndFixRotation(processed);

            Tesseract tesseract = new Tesseract();
            tesseract.setLanguage("eng");
            String datapath = System.getenv("TESSDATA_PREFIX");
            if (datapath != null && !datapath.isEmpty()) {
                tesseract.setDatapath(datapath);
            }
            tesseract.setPageSegMode(3);    // PSM_AUTO
            return tesseract.doOCR(processed);
        } catch (TesseractException e) {
            throw new RecipeImportException("OCR failed: " + e.getMessage(), e);
        }
    }

    private int detectRotationDegrees(BufferedImage image) {
        Path tempFile = null;
        try {
            BufferedImage gray = preprocessForOcr(image);
            tempFile = Files.createTempFile("osd-", ".png");
            ImageIO.write(gray, "png", tempFile.toFile());

            ProcessBuilder pb = new ProcessBuilder(
                    "tesseract", tempFile.toString(), "stdout", "--psm", "0");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            boolean finished = proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                log.debug("OSD process timed out after 10s");
                return 0;
            }

            return parseOsdRotation(output);
        } catch (Exception e) {
            log.debug("OSD rotation detection failed (non-fatal): {}", e.getMessage());
            return 0;
        } finally {
            try { if (tempFile != null) Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
        }
    }

    private BufferedImage detectAndFixRotation(BufferedImage image) {
        int rotation = detectRotationDegrees(image);
        if (rotation != 0) {
            log.info("OSD detected Rotate:{} — applying correction", rotation);
            return rotateImage(image, Math.toRadians(rotation));
        }
        return image;
    }

    private int parseOsdRotation(String osdOutput) {
        for (String line : osdOutput.split("\n")) {
            if (line.startsWith("Rotate:")) {
                String value = line.substring("Rotate:".length()).trim();
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private BufferedImage preprocessForOcr(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        int maxDim = 3000;
        if (w > maxDim || h > maxDim) {
            double scale = (double) maxDim / Math.max(w, h);
            int nw = (int) (w * scale);
            int nh = (int) (h * scale);
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(image, 0, 0, nw, nh, null);
            g.dispose();
            image = scaled;
            w = nw;
            h = nh;
        }

        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        gray.getGraphics().drawImage(image, 0, 0, null);
        return gray;
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

        String title = nonEmpty.get(0);

        int servings = 1;
        Matcher servMatcher = SERVINGS_PATTERN.matcher(text);
        if (servMatcher.find()) {
            servings = Integer.parseInt(servMatcher.group(1));
        }

        List<Map.Entry<String, String>> ingredientEntries = new ArrayList<>();
        List<String> instructionLines = new ArrayList<>();
        boolean inInstructions = false;
        String currentSection = null;

        for (int i = 1; i < nonEmpty.size(); i++) {
            String line = nonEmpty.get(i);
            String lower = line.toLowerCase();

            if (lower.matches("^(instructions?|directions?|method|steps?|preparation)\\s*:?\\s*$")) {
                inInstructions = true;
                continue;
            }
            if (lower.matches("^(ingredients?)\\s*:?\\s*$")) {
                inInstructions = false;
                ingredientEntries.clear();
                currentSection = null;
                continue;
            }

            if (SERVINGS_PATTERN.matcher(line).find() || TIME_PATTERN.matcher(line).find()) {
                continue;
            }

            if (METADATA_PATTERN.matcher(line).matches()) {
                continue;
            }

            if (!inInstructions) {
                if (SUB_SECTION_WITH_COLON.matcher(line).matches()) {
                    currentSection = cleanSectionName(line);
                    continue;
                }
                if (SUB_SECTION_SHORT_LABEL.matcher(line).matches()
                        && line.split("\\s+").length <= 4) {
                    currentSection = cleanSectionName(line);
                    continue;
                }
            }

            if (inInstructions) {
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

        if (instructionLines.isEmpty() && ingredientEntries.size() > 3) {
            int split = ingredientEntries.size() / 2;
            for (int i = split; i < ingredientEntries.size(); i++) {
                instructionLines.add(ingredientEntries.get(i).getValue());
            }
            ingredientEntries = new ArrayList<>(ingredientEntries.subList(0, split));
        }

        List<ImportedIngredientPreview> ingredients = ingredientEntries.stream()
                .filter(entry -> {
                    String cleaned = entry.getValue().trim().replaceFirst("^[^\\p{L}\\p{N}]+", "").trim();
                    return !cleaned.isEmpty();
                })
                .map(entry -> {
                    String section = entry.getKey();
                    String line = entry.getValue().trim().replaceFirst("^[^\\p{L}\\p{N}]+", "").trim();
                    ImportedIngredientPreview parsed = recipeImportService.parseIngredientText(line);
                    return new ImportedIngredientPreview(section, parsed.name(), parsed.quantity(), parsed.unit(), parsed.rawText(), null);
                })
                .toList();

        List<String> mergedInstructions = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        java.util.regex.Pattern inlineStepPattern = java.util.regex.Pattern.compile("^\\d+\\.\\s+([A-Z].+)");
        java.util.regex.Pattern bareNumberPattern = java.util.regex.Pattern.compile("^\\d+\\.\\s*$");
        java.util.regex.Pattern numPrefixPattern = java.util.regex.Pattern.compile("^\\d+\\.\\s*(.*)");
        java.util.regex.Pattern notesHeaderPattern = java.util.regex.Pattern.compile(
                "^(?:\\d+\\.\\s*)?(?i)(notes?|tips?|variations?)\\s*$");
        java.util.regex.Pattern bulletPattern = java.util.regex.Pattern.compile(
                "^(?:\\d+\\.\\s*)?[+*\\-\u2022]\\s*(.+)");

        long inlineCount = instructionLines.stream()
                .filter(l -> inlineStepPattern.matcher(l).matches())
                .count();
        long orphanedCount = instructionLines.stream()
                .filter(l -> bareNumberPattern.matcher(l).matches())
                .count();
        boolean useOrphanedMode = orphanedCount > inlineCount;
        boolean inNotes = false;

        for (String line : instructionLines) {
            if (notesHeaderPattern.matcher(line).matches()) {
                inNotes = true;
                continue;
            }

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

            if (bareNumberPattern.matcher(line).matches()) {
                continue;
            }

            if (useOrphanedMode) {
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


    /**
     * Lightweight MultipartFile wrapper for converted image bytes.
     */
    private record SimpleMultipartFile(String name, String contentType, byte[] bytes)
            implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
        @Override public void transferTo(java.io.File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
