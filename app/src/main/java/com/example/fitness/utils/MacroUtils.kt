package com.example.fitness.utils

import com.example.fitness.model.MacroTotals
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Fetches and aggregates all macro nutrients logged by a user for a given date.
 * It totals the calories, protein, carbs, and fat across all standard meal types.
 *
 * Optionally, the function also writes the summary back to Firestore under the "summary/daily" document.
 *
 * @param uid The user's unique identifier.
 * @param date The date in yyyy-MM-dd format (must match Firestore document structure).
 * @return MacroTotals object containing the summed macros for the day.
 */
suspend fun fetchMacroTotals(uid: String, date: String): MacroTotals {
    val db = FirebaseFirestore.getInstance()
    val userRef = db.collection("users").document(uid)

    // Standard meal categories expected in Firestore structure
    val allMeals = listOf("breakfast", "lunch", "dinner", "snacks")

    // Totals initialized to 0
    var totalCalories = 0
    var totalProtein = 0
    var totalCarbs = 0
    var totalFat = 0

    // Loop through each meal type and sum up the nutrient values
    for (meal in allMeals) {
        val snapshot = userRef
            .collection("meal_logs")
            .document(date)
            .collection(meal)
            .get()
            .await()

        for (doc in snapshot.documents) {
            totalCalories += (doc.getLong("calories") ?: 0).toInt()
            totalProtein += (doc.getLong("protein") ?: 0).toInt()
            totalCarbs += (doc.getLong("carbs") ?: 0).toInt()
            totalFat += (doc.getLong("fat") ?: 0).toInt()
        }
    }

    // Save the daily summary back to Firestore (can be used for quick reads or charts)
    val summaryMap = mapOf(
        "totalCalories" to totalCalories,
        "totalProtein" to totalProtein,
        "totalCarbs" to totalCarbs,
        "totalFat" to totalFat
    )
    userRef.collection("meal_logs").document(date)
        .collection("summary").document("daily").set(summaryMap)

    // Return the computed totals
    return MacroTotals(
        calories = totalCalories,
        protein = totalProtein,
        carbs = totalCarbs,
        fat = totalFat
    )
}
