package com.endoran.foodplan.model;

import com.endoran.foodplan.dto.ImportedRecipePreview;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "training_pairs")
public class TrainingPair {

    @Id
    private String id;
    private String orgId;
    private String scanSessionId;
    private byte[] imageData;
    private String imageContentType;
    private ImportedRecipePreview modelOutput;
    private ImportedRecipePreview correctedOutput;
    private String extractionTier;
    private boolean hasCorrections;
    private Instant createdAt;

    public TrainingPair() {
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public String getScanSessionId() { return scanSessionId; }
    public void setScanSessionId(String scanSessionId) { this.scanSessionId = scanSessionId; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }
    public ImportedRecipePreview getModelOutput() { return modelOutput; }
    public void setModelOutput(ImportedRecipePreview modelOutput) { this.modelOutput = modelOutput; }
    public ImportedRecipePreview getCorrectedOutput() { return correctedOutput; }
    public void setCorrectedOutput(ImportedRecipePreview correctedOutput) { this.correctedOutput = correctedOutput; }
    public String getExtractionTier() { return extractionTier; }
    public void setExtractionTier(String extractionTier) { this.extractionTier = extractionTier; }
    public boolean isHasCorrections() { return hasCorrections; }
    public void setHasCorrections(boolean hasCorrections) { this.hasCorrections = hasCorrections; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
