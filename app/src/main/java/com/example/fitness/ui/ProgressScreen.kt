package com.example.fitness.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.example.fitness.utils.TabButton
import com.patrykandpatrick.vico.core.extension.sumOf

@Composable
fun ProgressScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = Firebase.firestore

    var selectedTab by remember { mutableStateOf("progress") }   // Toggle between Progress and BMI
    var selectedMetric by remember { mutableStateOf("Weight") }  // Current metric shown
    var viewMode by remember { mutableStateOf("week") }          // "week" or "month" view

    var metricData by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch data whenever user ID, view mode, or metric changes
    LaunchedEffect(uid, viewMode, selectedMetric) {
        if (uid != null) {
            try {
                isLoading = true

                // Weight logs are simple
                if (selectedMetric == "Weight") {
                    val weightSnap = db.collection("users").document(uid)
                        .collection("weight_logs").get().await()

                    metricData = weightSnap.documents.mapNotNull {
                        val weight = it.getDouble("weight")?.toFloat() ?: return@mapNotNull null
                        val rawDate = it.id
                        val formatted = formatDate(rawDate, viewMode)
                        formatted to weight
                    }

                } else {
                    // Macronutrient metrics require fetching from meal_logs and summing
                    val mealLogs = db.collection("users").document(uid)
                        .collection("meal_logs").get().await()

                    val macroData = mutableListOf<Pair<String, Float>>()

                    for (log in mealLogs.documents) {
                        val dateLabel = formatDate(log.id, viewMode)

                        val foodDocs = db.collection("users").document(uid)
                            .collection("meal_logs").document(log.id)
                            .collection("foods").get().await()

                        val value = foodDocs.sumOf { food ->
                            when (selectedMetric) {
                                "Calories" -> food.getDouble("calories")?.toFloat() ?: 0f
                                "Protein" -> food.getDouble("protein")?.toFloat() ?: 0f
                                "Carbs" -> food.getDouble("carbs")?.toFloat() ?: 0f
                                "Fat" -> food.getDouble("fat")?.toFloat() ?: 0f
                                else -> 0f
                            }
                        }

                        macroData.add(dateLabel to value)
                    }

                    metricData = macroData
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Main UI column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Tab selection between Progress and BMI
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TabButton("Progress", selectedTab == "progress") { selectedTab = "progress" }
            TabButton("BMI", selectedTab == "bmi") { selectedTab = "bmi" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == "progress") {
            val metricTabs = listOf("Weight", "Calories", "Protein", "Carbs", "Fat")

            // Scrollable metrics row
            ScrollableTabRow(
                selectedTabIndex = metricTabs.indexOf(selectedMetric),
                edgePadding = 0.dp
            ) {
                metricTabs.forEach { label ->
                    Tab(
                        text = { Text(label) },
                        selected = selectedMetric == label,
                        onClick = { selectedMetric = label }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Toggle between weekly and monthly view
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = viewMode == "week",
                    onClick = { viewMode = "week" },
                    label = { Text("Week") }
                )
                FilterChip(
                    selected = viewMode == "month",
                    onClick = { viewMode = "month" },
                    label = { Text("Month") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                isLoading -> CircularProgressIndicator()
                metricData.isEmpty() -> Text("No $selectedMetric data found.")
                else -> {
                    // Select chart color based on metric
                    val color = when (selectedMetric) {
                        "Weight" -> AndroidColor.BLUE
                        "Calories" -> AndroidColor.RED
                        "Protein" -> AndroidColor.MAGENTA
                        "Carbs" -> AndroidColor.CYAN
                        "Fat" -> AndroidColor.GREEN
                        else -> AndroidColor.GRAY
                    }

                    LineChartView(metricData, selectedMetric, color)
                }
            }
        } else {
            // BMI View
            BMIScreen()
        }
    }
}

/**
 * Formats Firestore date strings based on view mode (week or month).
 */
fun formatDate(raw: String, viewMode: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(raw) ?: return raw
        val outputFormat = when (viewMode) {
            "week" -> SimpleDateFormat("EEE dd MMM", Locale.getDefault())  // Ex: Mon 06 May
            "month" -> SimpleDateFormat("MMM yyyy", Locale.getDefault())   // Ex: May 2025
            else -> SimpleDateFormat("dd MMM", Locale.getDefault())
        }
        outputFormat.format(date)
    } catch (e: Exception) {
        raw
    }
}

/**
 * Composable wrapper around MPAndroidChart's LineChart for Compose.
 */
@Composable
fun LineChartView(data: List<Pair<String, Float>>, label: String, color: Int) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.labelRotationAngle = -45f
                setTouchEnabled(true)
                setPinchZoom(true)
            }
        },
        update = { chart ->
            val dates = data.map { it.first }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)

            val entries = data.mapIndexed { index, (_, value) ->
                Entry(index.toFloat(), value)
            }

            val dataSet = LineDataSet(entries, label).apply {
                this.color = color
                valueTextColor = AndroidColor.DKGRAY
                lineWidth = 2f
                setDrawValues(false)
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(color)
            }

            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
