package com.endoran.foodplan.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "shared_recipes")
@CompoundIndex(name = "source_idx", def = "{'sourceInstanceId': 1, 'sourceRecipeId': 1}", unique = true)
public class SharedRecipe {

    @Id
    private String id;
    private String sourceInstanceId;
    private String sourceInstanceName;
    private String sourceOrgId;
    private String sourceRecipeId;
    private String name;
    private String instructions;
    private int baseServings;
    private List<SharedRecipeIngredient> ingredients = new ArrayList<>();
    private int version;
    private String attribution;
    private Instant sharedAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceInstanceId() { return sourceInstanceId; }
    public void setSourceInstanceId(String sourceInstanceId) { this.sourceInstanceId = sourceInstanceId; }

    public String getSourceInstanceName() { return sourceInstanceName; }
    public void setSourceInstanceName(String sourceInstanceName) { this.sourceInstanceName = sourceInstanceName; }

    public String getSourceOrgId() { return sourceOrgId; }
    public void setSourceOrgId(String sourceOrgId) { this.sourceOrgId = sourceOrgId; }

    public String getSourceRecipeId() { return sourceRecipeId; }
    public void setSourceRecipeId(String sourceRecipeId) { this.sourceRecipeId = sourceRecipeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public int getBaseServings() { return baseServings; }
    public void setBaseServings(int baseServings) { this.baseServings = baseServings; }

    public List<SharedRecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<SharedRecipeIngredient> ingredients) { this.ingredients = ingredients; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getAttribution() { return attribution; }
    public void setAttribution(String attribution) { this.attribution = attribution; }

    public Instant getSharedAt() { return sharedAt; }
    public void setSharedAt(Instant sharedAt) { this.sharedAt = sharedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
