package com.endoran.foodplan.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "pinned_recipes")
@CompoundIndex(name = "org_shared_idx", def = "{'orgId': 1, 'sharedRecipeId': 1}", unique = true)
public class PinnedRecipe {

    @Id
    private String id;
    private String orgId;
    private String sharedRecipeId;
    private int pinnedVersion;
    private String name;
    private String instructions;
    private int baseServings;
    private List<SharedRecipeIngredient> ingredients = new ArrayList<>();
    private String sourceInstanceName;
    private String attribution;
    private String localRecipeId;
    private Instant pinnedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getSharedRecipeId() { return sharedRecipeId; }
    public void setSharedRecipeId(String sharedRecipeId) { this.sharedRecipeId = sharedRecipeId; }

    public int getPinnedVersion() { return pinnedVersion; }
    public void setPinnedVersion(int pinnedVersion) { this.pinnedVersion = pinnedVersion; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public int getBaseServings() { return baseServings; }
    public void setBaseServings(int baseServings) { this.baseServings = baseServings; }

    public List<SharedRecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<SharedRecipeIngredient> ingredients) { this.ingredients = ingredients; }

    public String getSourceInstanceName() { return sourceInstanceName; }
    public void setSourceInstanceName(String sourceInstanceName) { this.sourceInstanceName = sourceInstanceName; }

    public String getAttribution() { return attribution; }
    public void setAttribution(String attribution) { this.attribution = attribution; }

    public String getLocalRecipeId() { return localRecipeId; }
    public void setLocalRecipeId(String localRecipeId) { this.localRecipeId = localRecipeId; }

    public Instant getPinnedAt() { return pinnedAt; }
    public void setPinnedAt(Instant pinnedAt) { this.pinnedAt = pinnedAt; }
}
