package com.example.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fitness.ui.MacroTrackerApp

/**
 * Main entry point of the Calz fitness tracking application.
 * This activity hosts the Jetpack Compose content using MacroTrackerApp.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sets the root composable UI to MacroTrackerApp (handles all screens and navigation)
        setContent {
            MacroTrackerApp()
        }
    }
}
