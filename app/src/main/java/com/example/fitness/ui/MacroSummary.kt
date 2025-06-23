package com.example.fitness.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitness.model.MacroItem

/**
 * A composable that shows a summary of total macros (calories, protein, carbs, fat)
 * for each meal type (e.g., Breakfast, Lunch, Dinner, Snacks).
 *
 * @param mealTypes A list of meal names like "Breakfast", "Lunch", etc.
 * @param loggedMeals A map from meal name to a list of macro items (food entries).
 */
@Composable
fun MacroSummary(
    mealTypes: List<String>,
    loggedMeals: Map<String, List<MacroItem>>
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // Emoji icons for each meal
        val mealIcons = mapOf(
            "Breakfast" to "🍳",
            "Lunch" to "🥪",
            "Dinner" to "🍽️",
            "Snacks" to "🍎"
        )

        // Loop through each meal and render its macro totals
        mealTypes.forEach { meal ->
            val items = loggedMeals[meal] ?: emptyList()

            // Aggregate macros
            val totalCalories = items.sumOf { it.calories }
            val totalProtein = items.sumOf { it.protein }
            val totalCarbs = items.sumOf { it.carbs }
            val totalFat = items.sumOf { it.fat }

            // Card UI for each meal summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Title with emoji
                    Text(
                        text = "${mealIcons[meal] ?: ""} $meal",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Macro stats
                    Text("Calories: $totalCalories kcal", style = MaterialTheme.typography.bodyMedium)
                    Text("Protein: $totalProtein g", style = MaterialTheme.typography.bodyMedium)
                    Text("Carbs: $totalCarbs g", style = MaterialTheme.typography.bodyMedium)
                    Text("Fat: $totalFat g", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
