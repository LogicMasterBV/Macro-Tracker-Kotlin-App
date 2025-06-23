package com.example.fitness.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitness.R
import com.example.fitness.model.UserProfile
import com.example.fitness.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate

/**
 * Full-screen settings view with background, editable profile, target macros, and logout.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 🔹 Background image for visual branding
        Image(
            painter = painterResource(id = R.drawable.background_calz),
            contentDescription = "Settings Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 🔹 Optional overlay to improve readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
        )

        // 🔹 Main screen content
        SettingsContent(navController)
    }
}

/**
 * Main content of the Settings screen: includes profile, targets, and logout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val db = Firebase.firestore

    // State for profile and target info
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditingProfile by remember { mutableStateOf(false) }

    // Editable fields
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var targetCalories by remember { mutableStateOf("") }
    var targetProtein by remember { mutableStateOf("") }
    var targetCarbs by remember { mutableStateOf("") }
    var targetFat by remember { mutableStateOf("") }

    // 🔄 Load user profile from Firestore
    LaunchedEffect(uid) {
        uid?.let {
            db.collection("users").document(it).get()
                .addOnSuccessListener { doc ->
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        userProfile = profile
                        name = profile.name
                        surname = profile.surname
                        age = profile.age.toString()
                        weight = profile.weight.toString()
                        height = profile.height.toString()
                        targetCalories = profile.targetCalories.toInt().toString()
                        targetProtein = profile.targetProtein.toInt().toString()
                        targetCarbs = profile.targetCarbs.toInt().toString()
                        targetFat = profile.targetFat.toInt().toString()
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    showToast(context, "Failed to load profile")
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // 🔹 Tabs: Profile and Targets
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Profile") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Targets") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Profile tab content
            if (selectedTab == 0) {
                if (isEditingProfile) {
                    ProfileSection(
                        name, { name = it },
                        surname, { surname = it },
                        age, { age = it },
                        weight, { weight = it },
                        height, { height = it },
                        onSave = {
                            val updated = UserProfile(
                                uid = uid ?: "",
                                name = name,
                                surname = surname,
                                age = age.toIntOrNull() ?: 0,
                                weight = weight.toFloatOrNull() ?: 0f,
                                height = height.toFloatOrNull() ?: 0f,
                                email = userProfile?.email ?: "",
                                targetCalories = targetCalories.toFloatOrNull() ?: 2000f,
                                targetProtein = targetProtein.toFloatOrNull() ?: 100f,
                                targetCarbs = targetCarbs.toFloatOrNull() ?: 250f,
                                targetFat = targetFat.toFloatOrNull() ?: 70f
                            )

                            db.collection("users").document(uid!!).set(updated)
                                .addOnSuccessListener {
                                    userProfile = updated
                                    showToast(context, "Profile updated")
                                    isEditingProfile = false

                                    // Automatically log weight for today
                                    val today = LocalDate.now().toString()
                                    val weightLog = mapOf("date" to today, "weight" to updated.weight)
                                    db.collection("users").document(uid).collection("weight_logs")
                                        .document(today).set(weightLog)
                                }
                                .addOnFailureListener {
                                    showToast(context, "Failed to update profile")
                                }
                        }
                    )
                } else {
                    userProfile?.let {
                        Text("Name: ${it.name}")
                        Text("Surname: ${it.surname}")
                        Text("Age: ${it.age}")
                        Text("Weight: ${it.weight} kg")
                        Text("Height: ${it.height} cm")
                        Text("Email: ${it.email}")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { isEditingProfile = true }) {
                            Text("Edit Profile")
                        }
                    } ?: Text("No user data found")
                }
            } else {
                // 🔹 Macro target tab content
                TargetSection(
                    targetCalories, { targetCalories = it },
                    targetProtein, { targetProtein = it },
                    targetCarbs, { targetCarbs = it },
                    targetFat, { targetFat = it },
                    onSave = {
                        userProfile?.let {
                            val updated = it.copy(
                                targetCalories = targetCalories.toFloatOrNull() ?: 2000f,
                                targetProtein = targetProtein.toFloatOrNull() ?: 100f,
                                targetCarbs = targetCarbs.toFloatOrNull() ?: 250f,
                                targetFat = targetFat.toFloatOrNull() ?: 70f
                            )
                            db.collection("users").document(uid!!).set(updated)
                                .addOnSuccessListener {
                                    userProfile = updated
                                    showToast(context, "Targets updated")
                                }
                                .addOnFailureListener {
                                    showToast(context, "Failed to update targets")
                                }
                        }
                    }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Spacer(Modifier.height(16.dp))

            // 🔹 Logout with confirmation dialog
            LogoutItemWithConfirmation {
                auth.signOut()
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }
}

/**
 * Editable user profile fields (name, age, etc.)
 */
@Composable
fun ProfileSection(
    name: String, onNameChange: (String) -> Unit,
    surname: String, onSurnameChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    onSave: () -> Unit
) {
    OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Name") })
    OutlinedTextField(value = surname, onValueChange = onSurnameChange, label = { Text("Surname") })
    OutlinedTextField(value = age, onValueChange = onAgeChange, label = { Text("Age") })
    OutlinedTextField(value = weight, onValueChange = onWeightChange, label = { Text("Weight (kg)") })
    OutlinedTextField(value = height, onValueChange = onHeightChange, label = { Text("Height (cm)") })
    Spacer(Modifier.height(12.dp))
    Button(onClick = onSave) { Text("Save Profile") }
}

/**
 * Macro target editor section (calories, protein, etc.)
 */
@Composable
fun TargetSection(
    targetCalories: String, onCaloriesChange: (String) -> Unit,
    targetProtein: String, onProteinChange: (String) -> Unit,
    targetCarbs: String, onCarbsChange: (String) -> Unit,
    targetFat: String, onFatChange: (String) -> Unit,
    onSave: () -> Unit
) {
    OutlinedTextField(value = targetCalories, onValueChange = onCaloriesChange, label = { Text("Target Calories") })
    OutlinedTextField(value = targetProtein, onValueChange = onProteinChange, label = { Text("Target Protein") })
    OutlinedTextField(value = targetCarbs, onValueChange = onCarbsChange, label = { Text("Target Carbs") })
    OutlinedTextField(value = targetFat, onValueChange = onFatChange, label = { Text("Target Fat") })
    Spacer(Modifier.height(12.dp))
    Button(onClick = onSave) { Text("Save Targets") }
}

/**
 * Logout menu item with confirmation dialog.
 */
@Composable
fun LogoutItemWithConfirmation(onLogoutConfirmed: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    SettingItem(title = "Logout") {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onLogoutConfirmed()
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Reusable clickable row for settings items like "Logout"
 */
@Composable
fun SettingItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}
