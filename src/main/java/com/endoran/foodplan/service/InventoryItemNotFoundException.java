package com.endoran.foodplan.service;

public class InventoryItemNotFoundException extends RuntimeException {
    public InventoryItemNotFoundException(String id) {
        super("Inventory item not found: " + id);
    }
}
