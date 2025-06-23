package com.example.fitness.model

/**
 * Represents the total nutritional intake for a specific day.
 * Used for summary views such as the Home screen.
 */
data class MacroTotals(
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
)

/**
 * Represents a single food item's nutritional values.
 * Used to build up totals for a meal or a summary.
 */
data class MacroItem(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

/**
 * Represents a food item that has been logged in a meal.
 * Includes a document ID for editing or deletion in Firestore.
 */
data class MealItem(
    val docId: String,
    val name: String,
    val quantity: Int,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

/**
 * Represents a food item returned from the USDA API.
 * This is used when searching for new foods to log.
 */
data class FoodItem (
    val name: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
)
