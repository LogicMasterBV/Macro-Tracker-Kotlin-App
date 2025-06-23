// SplashScreen.kt
package com.example.fitness.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

/**
 * A simple splash screen that checks if the user is authenticated
 * and navigates to either the home screen or login screen accordingly.
 *
 * This screen is briefly shown while authentication state is validated.
 */
@Composable
fun SplashScreen(navController: NavController) {
    // Launch side effect when the composable enters the composition
    LaunchedEffect(Unit) {
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

        // Navigate to the appropriate screen and remove SplashScreen from the back stack
        navController.navigate(if (isLoggedIn) "home" else "login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    // UI shown while authentication state is being checked
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator() // Loading indicator
    }
}
