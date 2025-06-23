// File: com/example/fitness/ui/utils/TabButton.kt

package com.example.fitness.utils

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * A reusable tab-style button for toggling between states or views.
 *
 * This composable is typically used in screens like Log or Progress
 * where users switch between different views (e.g., "Log" vs "Summary", "Progress" vs "BMI").
 *
 * @param title The text label displayed on the button.
 * @param selected Indicates whether this tab is currently selected.
 * @param onClick Callback invoked when the button is clicked.
 */
@Composable
fun TabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        // Use primary color if selected, otherwise a neutral surface variant
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(title)
    }
}
