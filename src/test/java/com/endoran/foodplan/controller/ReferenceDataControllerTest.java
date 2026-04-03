package com.endoran.foodplan.controller;

import com.endoran.foodplan.model.DietaryTag;
import com.endoran.foodplan.model.GroceryCategory;
import com.endoran.foodplan.model.MeasurementUnit;
import com.endoran.foodplan.model.StorageCategory;
import com.endoran.foodplan.service.SlackErrorNotifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReferenceDataController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReferenceDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SlackErrorNotifier slackErrorNotifier;

    @Test
    void getMeasurementUnitsReturnsAllValues() throws Exception {
        mockMvc.perform(get("/api/v1/reference/measurement-units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(MeasurementUnit.values().length))
                .andExpect(jsonPath("$[0]").value("TSP"));
    }

    @Test
    void getStorageCategoriesReturnsAllValues() throws Exception {
        mockMvc.perform(get("/api/v1/reference/storage-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(StorageCategory.values().length))
                .andExpect(jsonPath("$[0]").value("PANTRY"));
    }

    @Test
    void getGroceryCategoriesReturnsAllValues() throws Exception {
        mockMvc.perform(get("/api/v1/reference/grocery-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(GroceryCategory.values().length))
                .andExpect(jsonPath("$[0]").value("PRODUCE"));
    }

    @Test
    void getDietaryTagsReturnsAllValues() throws Exception {
        mockMvc.perform(get("/api/v1/reference/dietary-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(DietaryTag.values().length))
                .andExpect(jsonPath("$[0]").value("GLUTEN_FREE"));
    }
}
