package com.endoran.foodplan.service;

import java.util.Set;

public final class IngredientNameNormalizer {

    private static final Set<String> LOWERCASE_WORDS = Set.of(
            "of", "and", "with", "in", "for", "the", "a", "an", "or");

    private IngredientNameNormalizer() {}

    public static String normalize(String name) {
        if (name == null || name.isBlank()) return name;

        String cleaned = name.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[.,;:!]+$", "");

        if (cleaned.isEmpty()) return cleaned;

        StringBuilder result = new StringBuilder();
        boolean inParens = false;
        String[] words = cleaned.split(" ");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isEmpty()) continue;

            if (result.length() > 0) result.append(' ');

            if (word.startsWith("(")) {
                inParens = true;
                result.append('(');
                word = word.substring(1);
                if (word.isEmpty()) continue;
            }

            boolean endsParens = word.endsWith(")");
            if (endsParens) {
                word = word.substring(0, word.length() - 1);
            }

            if (i > 0 && !inParens && LOWERCASE_WORDS.contains(word.toLowerCase())) {
                result.append(word.toLowerCase());
            } else {
                result.append(titleCase(word));
            }

            if (endsParens) {
                result.append(')');
                inParens = false;
            }
        }

        return result.toString();
    }

    private static String titleCase(String word) {
        if (word.length() == 1) return word.toUpperCase();
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }
}
