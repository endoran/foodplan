package com.endoran.foodplan.repository;

import com.endoran.foodplan.model.MealPlanEntry;
import com.endoran.foodplan.model.MealType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface MealPlanEntryRepository extends MongoRepository<MealPlanEntry, String> {

    List<MealPlanEntry> findByOrgId(String orgId);

    @Query("{ 'orgId': ?0, 'date': { $gte: ?1, $lte: ?2 } }")
    List<MealPlanEntry> findByOrgIdAndDateRange(String orgId, LocalDate from, LocalDate to);

    @Query("{ 'orgId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'mealType': ?3 }")
    List<MealPlanEntry> findByOrgIdAndDateRangeAndMealType(String orgId, LocalDate from, LocalDate to, MealType mealType);
}
