package com.example.fitness.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.fitness.R
import com.example.fitness.utils.AddFoodScreenWithQuery
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

/**
 * The root composable that sets up navigation, background image, and main layout.
 */
@Composable
fun MacroTrackerApp() {
    val navController = rememberNavController()

    // Determine if the user is logged in; navigate to home or splash accordingly
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "home" else "splash"

    Box(modifier = Modifier.fillMaxSize()) {
        // 🔹 Background image for the app
        Image(
            painter = painterResource(id = R.drawable.background_calz),
            contentDescription = "App Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for darkening background slightly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
        )

        // Main app scaffold
        Scaffold(
            bottomBar = {
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                // Hide bottom bar on auth-related screens
                val hideBottomBarRoutes = listOf("login", "register", "forgot_password", "splash")
                if (currentRoute !in hideBottomBarRoutes) {
                    BottomNavigationBar(navController)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { padding ->

            // Define the app's navigation graph
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(padding)
            ) {
                // Auth flow
                composable("splash") { SplashScreen(navController) }
                composable("login") { LoginScreen(navController) }
                composable("register") { RegisterScreen(navController) }
                composable("forgot_password") { ForgotPasswordScreen(navController) }

                // Main app screens
                composable("home") { HomeScreen() }
                composable("log") { LogScreen(navController) }
                composable("camera") { CameraScreen(navController) }
                composable("progress") { ProgressScreen() }
                composable("settings") { SettingsScreen(navController) }

                // Add food screens
                composable("add_food/{mealType}/{date}") { backStackEntry ->
                    val mealType = backStackEntry.arguments?.getString("mealType") ?: "unknown"
                    val date = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
                    AddFoodScreen(mealType = mealType, date = date, navController = navController)
                }

                // Image-detected food screen with prefilled query
                composable("add_food_detected/{mealType}/{query}") { backStackEntry ->
                    val mealType = backStackEntry.arguments?.getString("mealType") ?: "snacks"
                    val query = backStackEntry.arguments?.getString("query") ?: ""
                    AddFoodScreenWithQuery(initialQuery = query, mealType = mealType, navController = navController)
                }
            }
        }
    }
}

/**
 * Bottom navigation bar for main sections: Home, Log, Progress, Settings
 * Includes a center Floating Action Button for Camera access.
 */
@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Arrange icons around center FAB
    val leftItems = listOf("home", "log")
    val rightItems = listOf("progress", "settings")

    val icons = mapOf(
        "home" to Icons.Default.Home,
        "log" to Icons.Default.Restaurant,
        "progress" to Icons.AutoMirrored.Filled.ShowChart,
        "settings" to Icons.Default.Settings
    )

    NavigationBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left-aligned icons
            leftItems.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(icons[screen]!!, contentDescription = screen) },
                    selected = currentRoute == screen,
                    onClick = {
                        navController.navigate(screen) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    label = { Text(screen.replaceFirstChar { it.uppercase() }) }
                )
            }

            // Center camera button (FAB)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .offset(y = (-16).dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate("camera") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Camera",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Right-aligned icons
            rightItems.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(icons[screen]!!, contentDescription = screen) },
                    selected = currentRoute == screen,
                    onClick = {
                        navController.navigate(screen) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    label = { Text(screen.replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}
