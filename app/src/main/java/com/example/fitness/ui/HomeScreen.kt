package com.example.fitness.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitness.model.UserProfile
import com.example.fitness.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen() {
    val auth = FirebaseAuth.getInstance()
    val db = Firebase.firestore
    val uid = auth.currentUser?.uid

    // Today's date, formatted and stringified
    val today = LocalDate.now()
    val todayStr = today.toString()
    val formattedDate = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }

    // Firebase user profile
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // ViewModel provides daily macro totals from Firestore
    val viewModel: HomeViewModel = viewModel()
    val dailyTotals by viewModel.totals.collectAsState()

    // Load user profile and daily totals
    LaunchedEffect(uid) {
        if (uid != null) {
            val userRef = db.collection("users").document(uid)
            userRef.get().addOnSuccessListener {
                userProfile = it.toObject(UserProfile::class.java)
            }
            viewModel.load(uid, todayStr)
            isLoading = false
        }
    }

    // Main layout container
    Box(modifier = Modifier.fillMaxSize()) {

        if (isLoading) {
            // Loading indicator while fetching data
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            userProfile?.let { profile ->
                // Main content when user profile is loaded
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Greeting with name
                    Text(
                        text = "👋 Welcome back, ${profile.name}!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Display formatted date
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Calories progress card (full-width)
                    MacroRectCard(
                        title = "Calories",
                        current = dailyTotals.calories,
                        target = profile.targetCalories.toInt(),
                        emoji = "🔥",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Protein and Carbs in horizontal row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MacroRectCard(
                            title = "Protein",
                            current = dailyTotals.protein,
                            target = profile.targetProtein.toInt(),
                            emoji = "🍗",
                            modifier = Modifier.weight(1f)
                        )
                        MacroRectCard(
                            title = "Carbs",
                            current = dailyTotals.carbs,
                            target = profile.targetCarbs.toInt(),
                            emoji = "🍞",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fat + spacer for alignment
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MacroRectCard(
                            title = "Fat",
                            current = dailyTotals.fat,
                            target = profile.targetFat.toInt(),
                            emoji = "🧈",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f)) // empty to balance layout
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Footer note
                    Text(
                        "⏱ Synced just now",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Fallback if profile is still loading or null
                Text("Loading user profile...")
            }
        }
    }
}

@Composable
fun MacroRectCard(
    title: String,
    current: Int,
    target: Int,
    emoji: String,
    modifier: Modifier = Modifier
) {
    // Progress animation (clamped between 0–1)
    val progress by animateFloatAsState(
        targetValue = (current / target.toFloat()).coerceIn(0f, 1f),
        label = "$title-progress"
    )

    // Card layout
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title and values
            Column {
                Text("$emoji $title", style = MaterialTheme.typography.titleMedium)
                Text("$current / $target", style = MaterialTheme.typography.bodyMedium)
            }

            // Circular progress in top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
