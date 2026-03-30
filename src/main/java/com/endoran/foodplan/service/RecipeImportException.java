package com.endoran.foodplan.service;

public class RecipeImportException extends RuntimeException {
    public RecipeImportException(String message) {
        super(message);
    }

    public RecipeImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
