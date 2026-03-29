package com.endoran.foodplan.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "meal_plan_entries")
public class MealPlanEntry {

    @Id
    private String id;
    private String orgId;
    private LocalDate date;
    private MealType mealType;
    private String recipeId;
    private String recipeName;
    private int servings = 1;
    private String notes;
    private MealStatus status = MealStatus.PLANNED;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public MealType getMealType() { return mealType; }
    public void setMealType(MealType mealType) { this.mealType = mealType; }

    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    public String getRecipeName() { return recipeName; }
    public void setRecipeName(String recipeName) { this.recipeName = recipeName; }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public MealStatus getStatus() { return status; }
    public void setStatus(MealStatus status) { this.status = status; }
}
