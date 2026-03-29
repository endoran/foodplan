package com.endoran.foodplan.controller;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.MeasurementUnit;
import com.endoran.foodplan.model.StorageCategory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceDataController {

    @GetMapping("/measurement-units")
    public MeasurementUnit[] getMeasurementUnits() {
        return MeasurementUnit.values();
    }

    @GetMapping("/storage-categories")
    public StorageCategory[] getStorageCategories() {
        return StorageCategory.values();
    }

    @GetMapping("/grocery-categories")
    public GroceryCategory[] getGroceryCategories() {
        return GroceryCategory.values();
    }

    @GetMapping("/dietary-tags")
    public DietaryTag[] getDietaryTags() {
        return DietaryTag.values();
    }
}
