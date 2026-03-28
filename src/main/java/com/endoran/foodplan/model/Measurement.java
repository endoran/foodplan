package com.endoran.foodplan.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Measurement {

    private BigDecimal quantity;
    private MeasurementUnit unit;

    public Measurement() {
    }

    public Measurement(BigDecimal quantity, MeasurementUnit unit) {
        this.quantity = quantity;
        this.unit = unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public MeasurementUnit getUnit() {
        return unit;
    }

    public void setUnit(MeasurementUnit unit) {
        this.unit = unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Measurement that = (Measurement) o;
        return Objects.equals(quantity, that.quantity) && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity, unit);
    }
}
