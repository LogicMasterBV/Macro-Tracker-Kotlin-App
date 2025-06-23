// File: com/example/fitness/utils/AddFoodScreenWithQuery.kt

package com.example.fitness.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.fitness.ui.AddFoodScreen
import java.time.LocalDate

/**
 * A utility Composable that wraps the AddFoodScreen,
 * providing a convenient way to launch it with a predefined search query.
 *
 * This is typically used when a food label is detected via image recognition
 * and the app needs to directly display search results for that label.
 *
 * @param initialQuery The text to prefill the search field (e.g., a food label).
 * @param mealType The meal category (e.g., "breakfast", "lunch", etc.).
 * @param navController The navigation controller used to manage screen transitions.
 */
@Composable
fun AddFoodScreenWithQuery(
    initialQuery: String,
    mealType: String,
    navController: NavHostController
) {
    // Use the current date in ISO format (yyyy-MM-dd)
    val today = LocalDate.now().toString()

    // Launch the standard AddFoodScreen but with query and date pre-filled
    AddFoodScreen(
        mealType = mealType,
        date = today,
        navController = navController,
        initialQuery = initialQuery
    )
}
