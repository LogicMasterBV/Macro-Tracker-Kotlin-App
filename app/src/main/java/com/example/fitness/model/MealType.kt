package com.example.fitness.model

/**
 * Enum representing the four types of meals tracked in the app.
 * Each entry includes a lowercase label used for Firestore storage paths and UI routing.
 */
enum class MealType(val label: String) {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
    SNACKS("snacks");

    companion object {
        /**
         * Converts a string label back into a MealType enum value.
         * Useful for parsing Firestore paths or navigation arguments.
         *
         * @param label the string label (e.g. "breakfast")
         * @return the corresponding MealType, or null if not found
         */
        fun from(label: String) = entries.firstOrNull { it.label == label }
    }
}
