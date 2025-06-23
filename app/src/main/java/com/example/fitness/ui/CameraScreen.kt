package com.example.fitness.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.fitness.BuildConfig
import com.example.fitness.model.searchUSDA
import com.example.fitness.utils.analyzeImageWithVisionApi
import com.example.fitness.utils.parseLabels
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(navController: NavHostController) {
    val context = LocalContext.current

    // State variables
    var hasCameraPermission by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var verifiedLabels by remember { mutableStateOf<List<String>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val mealOptions = listOf("breakfast", "lunch", "dinner", "snacks")
    var selectedMealType by remember { mutableStateOf(mealOptions.first()) }

    // Request camera permission on first launch
    LaunchedEffect(Unit) {
        val permissionResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        hasCameraPermission = permissionResult == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            ActivityCompat.requestPermissions(
                (context as android.app.Activity),
                arrayOf(Manifest.permission.CAMERA),
                0
            )
        }
    }

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera & Food Recognition", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        // Dropdown to choose meal type
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Meal: ", style = MaterialTheme.typography.titleMedium)
            DropdownMenuBox(
                mealOptions = mealOptions,
                selected = selectedMealType,
                onSelect = { selectedMealType = it }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!hasCameraPermission) {
            Text("Camera permission is required.")
        } else {
            if (capturedBitmap == null) {
                // Show live camera preview
                CameraPreview(
                    onImageCaptured = { bitmap ->
                        capturedBitmap = bitmap

                        // Start Vision + USDA processing
                        coroutineScope.launch {
                            isProcessing = true
                            try {
                                // Step 1: Detect food labels using Vision API
                                val resultJson = analyzeImageWithVisionApi(bitmap)
                                val rawLabels = parseLabels(resultJson)

                                // Step 2: Verify detected labels against USDA search
                                val verified = mutableListOf<String>()
                                for (label in rawLabels) {
                                    val usdaResults = searchUSDA(label, BuildConfig.USDA_API_KEY)
                                    if (usdaResults.isNotEmpty()) {
                                        verified.add(label)
                                    }
                                }

                                // Save verified results
                                verifiedLabels = verified
                            } catch (e: Exception) {
                                Toast.makeText(context, "Vision check failed", Toast.LENGTH_SHORT).show()
                            }
                            isProcessing = false
                        }
                    }
                )
            } else {
                // Show captured photo and retake option
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured image",
                    modifier = Modifier
                        .size(280.dp)
                        .padding(8.dp)
                )
                Button(onClick = {
                    capturedBitmap = null
                    verifiedLabels = emptyList()
                }) {
                    Text("Retake")
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                isProcessing -> {
                    // Show progress indicator while processing
                    CircularProgressIndicator()
                }

                verifiedLabels.isNotEmpty() -> {
                    // Show verified results from Vision + USDA
                    Text(
                        "Verified Foods:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(verifiedLabels) { label ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Add button links to add_food_detected screen
                                    Button(onClick = {
                                        navController.navigate("add_food_detected/${selectedMealType}/${label}")
                                    }) {
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable dropdown menu for meal type selection.
 */
@Composable
fun DropdownMenuBox(mealOptions: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selected.replaceFirstChar { it.uppercaseChar() })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            mealOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercaseChar() }) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
