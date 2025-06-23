package com.example.fitness.model

/**
 * Represents a user's profile information and nutritional targets.
 * Stored in Firestore under the "users" collection.
 */
data class UserProfile(
    val uid: String = "",           // Unique Firebase user ID
    val name: String = "",          // First name of the user
    val surname: String = "",       // Last name of the user
    val age: Int = 0,               // Age in years
    val weight: Float = 0f,         // Current weight in kilograms
    val height: Float = 0f,         // Height in centimeters
    val email: String = "",         // User's email (used for login & contact)

    // Default daily macro targets, editable from the Settings screen
    var targetCalories: Float = 2000f, // Daily energy goal in kcal
    var targetProtein: Float = 100f,   // Daily protein goal in grams
    var targetCarbs: Float = 250f,     // Daily carbohydrate goal in grams
    var targetFat: Float = 70f         // Daily fat goal in grams
)
