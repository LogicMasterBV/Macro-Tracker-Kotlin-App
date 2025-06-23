package com.example.fitness.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun BMIScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = Firebase.firestore

    // List of date -> BMI values
    var bmiHistory by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }

    // User height in centimeters (needed to calculate BMI)
    var height by remember { mutableStateOf<Float?>(null) }

    // Loading state
    var isLoading by remember { mutableStateOf(true) }

    // Fetch BMI data once when the screen is first composed
    LaunchedEffect(uid) {
        if (uid != null) {
            try {
                // Get user height from main user document
                val userDoc = db.collection("users").document(uid).get().await()
                height = userDoc.getDouble("height")?.toFloat()

                // Get weight logs from subcollection
                val weightLogs = db.collection("users").document(uid)
                    .collection("weight_logs").get().await()

                // Calculate BMI for each weight log
                bmiHistory = weightLogs.documents.mapNotNull {
                    val weight = it.getDouble("weight")?.toFloat() ?: return@mapNotNull null
                    val date = formatDate(it.id)
                    val bmi = if (height != null) weight / ((height!! / 100) * (height!! / 100)) else return@mapNotNull null
                    date to bmi
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // UI Layout
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)) {

        Text("BMI History", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> CircularProgressIndicator()
            bmiHistory.isEmpty() -> Text("No BMI data available.")
            else -> {
                LineChartView_BMI(bmiHistory) // BMI Line Chart
                Spacer(modifier = Modifier.height(24.dp))
                BMIStatusBar(latestBMI = bmiHistory.lastOrNull()?.second) // Colored category bar
            }
        }
    }
}

@Composable
fun LineChartView_BMI(data: List<Pair<String, Float>>) {
    // Chart rendered using AndroidView interoperability (MPAndroidChart)
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
            // Format chart data
            val dates = data.map { it.first }
            val entries = data.mapIndexed { index, pair ->
                Entry(index.toFloat(), pair.second)
            }

            // Line configuration
            val dataSet = LineDataSet(entries, "BMI").apply {
                color = AndroidColor.MAGENTA
                valueTextColor = AndroidColor.DKGRAY
                lineWidth = 2f
                setDrawValues(false)
                setDrawCircles(true)
                circleRadius = 4f
                setCircleColor(AndroidColor.MAGENTA)
            }

            // Apply labels to X-axis
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
            chart.data = LineData(dataSet)
            chart.invalidate() // Force redraw
        }
    )
}

@Composable
fun BMIStatusBar(latestBMI: Float?) {
    // Define color-coded BMI categories
    val ranges = listOf(
        "Underweight" to Pair(Color(0xFF42A5F5), 0f..18.4f),
        "Normal" to Pair(Color(0xFF66BB6A), 18.5f..24.9f),
        "Overweight" to Pair(Color(0xFFFFA726), 25f..29.9f),
        "Obese" to Pair(Color(0xFFEF5350), 30f..50f)
    )

    val totalWidth = 300.dp
    val maxBmi = 50f // Max value for positioning

    Column {
        Text("BMI Categories", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Colored segmented bar for categories
        Box(modifier = Modifier.width(totalWidth).height(30.dp)) {
            Row(modifier = Modifier.fillMaxSize()) {
                ranges.forEach { (_, pair) ->
                    val color = pair.first
                    val weight = pair.second.endInclusive - pair.second.start + 1
                    val portion = weight / maxBmi
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(portion)
                            .background(color)
                    )
                }
            }

            // Marker line showing current BMI
            latestBMI?.let {
                val offsetFraction = (it / maxBmi).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .offset(x = totalWidth * offsetFraction - 1.dp)
                        .background(Color.Black)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label text under each category
        Row(Modifier.width(totalWidth), horizontalArrangement = Arrangement.SpaceBetween) {
            ranges.forEach { (label, _) ->
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }

        // Show BMI value
        latestBMI?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your latest BMI: %.1f".format(it), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// Formats Firestore doc IDs like "2025-05-01" to "01 May"
fun formatDate(raw: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val output = SimpleDateFormat("dd MMM", Locale.getDefault())
        val date = sdf.parse(raw)
        output.format(date ?: Date())
    } catch (e: Exception) {
        raw
    }
}
