package com.endoran.foodplan.model;

import com.endoran.foodplan.dto.ImportedRecipePreview;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "scan_sessions")
public class ScanSession {

    @Id
    private String id;
    private String orgId;
    private byte[] imageData;
    private String imageContentType;
    private List<ImportedRecipePreview> modelOutput;
    private String extractionTier;

    @Indexed(expireAfterSeconds = 86400)
    private Instant createdAt;

    public ScanSession() {
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }
    public List<ImportedRecipePreview> getModelOutput() { return modelOutput; }
    public void setModelOutput(List<ImportedRecipePreview> modelOutput) { this.modelOutput = modelOutput; }
    public String getExtractionTier() { return extractionTier; }
    public void setExtractionTier(String extractionTier) { this.extractionTier = extractionTier; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
