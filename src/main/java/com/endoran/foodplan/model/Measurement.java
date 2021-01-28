package com.endoran.foodplan.model;

public class Measurement {
    private float quantity;
    private MeasurementCategory measurementCategory;

    public Measurement(int quantity, MeasurementCategory measurementCategory) {
        this.quantity = quantity;
        this.measurementCategory = measurementCategory;
    }

    public float getQuantity() {
        return quantity;
    }

    public MeasurementCategory getMeasurementCategory() {
        return measurementCategory;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public void setMeasurementCategory(MeasurementCategory measurementCategory) {
        this.measurementCategory = measurementCategory;
    }
}
