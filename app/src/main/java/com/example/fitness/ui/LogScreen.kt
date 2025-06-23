package com.example.fitness.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitness.model.MacroItem
import com.example.fitness.model.MealItem
import com.example.fitness.model.MealType
import com.example.fitness.utils.TabButton
import com.example.fitness.utils.fetchMacroTotals
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun LogScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid
    val today = remember { LocalDate.now() }

    // Holds the type of meals (Breakfast, Lunch, etc.)
    val mealTypes = MealType.entries

    // Maps to store meals, notes, expanded states, and Firestore listeners
    val loggedMeals = remember { mutableStateMapOf<String, List<MealItem>>() }
    val mealNotes = remember { mutableStateMapOf<String, String>() }
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val listeners = remember { mutableStateMapOf<String, ListenerRegistration>() }

    // Selected date for logging, and which tab is currently shown
    var selectedDate by remember { mutableStateOf(today) }
    var selectedTab by remember { mutableStateOf("log") }

    // Triggered when macros need to be recalculated after changes
    fun updateSummary(uid: String, date: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                fetchMacroTotals(uid, date)
            } catch (e: Exception) {
                Log.e("LogScreen", "Failed to update summary: ${e.message}", e)
            }
        }
    }

    // Initializes expanded state for each meal
    mealTypes.forEach { meal ->
        val displayName = meal.name.lowercase().replaceFirstChar { it.uppercase() }
        if (expandedStates[displayName] == null) expandedStates[displayName] = true
    }

    // Loads meal items and notes for the selected date from Firestore
    fun loadMeals() {
        // Remove previous listeners to avoid memory leaks
        listeners.values.forEach { it.remove() }
        listeners.clear()

        if (uid != null) {
            mealTypes.forEach { meal ->
                val displayName = meal.name.lowercase().replaceFirstChar { it.uppercase() }

                // Listen for live changes in meal items
                val listener = db.collection("users")
                    .document(uid)
                    .collection("meal_logs")
                    .document(selectedDate.toString())
                    .collection(meal.label)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        val items = snapshot.documents.mapNotNull {
                            val id = it.id
                            val name = it.getString("name") ?: return@mapNotNull null
                            val quantity = it.getLong("quantity")?.toInt() ?: 1
                            val calories = it.getLong("calories")?.toInt() ?: 0
                            val protein = it.getLong("protein")?.toInt() ?: 0
                            val carbs = it.getLong("carbs")?.toInt() ?: 0
                            val fat = it.getLong("fat")?.toInt() ?: 0
                            MealItem(id, name, quantity, calories, protein, carbs, fat)
                        }
                        loggedMeals[displayName] = items
                    }
                listeners[displayName] = listener
            }

            // Load notes for each meal type
            db.collection("users")
                .document(uid)
                .collection("meal_logs")
                .document(selectedDate.toString())
                .get()
                .addOnSuccessListener { doc ->
                    mealTypes.forEach { meal ->
                        val displayName = meal.name.lowercase().replaceFirstChar { it.uppercase() }
                        val note = doc.getString("${meal.label}_note") ?: ""
                        mealNotes[displayName] = note
                    }
                }
        }
    }

    // Run loadMeals whenever the user or selectedDate changes
    LaunchedEffect(uid, selectedDate) { loadMeals() }

    // Remove listeners on composable disposal
    DisposableEffect(Unit) {
        onDispose { listeners.values.forEach { it.remove() } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top navigation tabs: Meal Log / Summary
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TabButton("Meal Log", selectedTab == "log") { selectedTab = "log" }
            TabButton("Summary", selectedTab == "summary") { selectedTab = "summary" }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display macro summary if in summary tab
        if (selectedTab == "summary") {
            val macroMeals = mealTypes.associate { meal ->
                val displayName = meal.name.lowercase().replaceFirstChar { it.uppercase() }
                displayName to (loggedMeals[displayName] ?: listOf()).map {
                    MacroItem(it.calories, it.protein, it.carbs, it.fat)
                }
            }
            MacroSummary(macroMeals.keys.toList(), macroMeals)
        }

        // Meal logging section
        if (selectedTab == "log") {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date navigation header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
                        }

                        Text(
                            if (selectedDate == today) "Today"
                            else selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                        }
                    }
                }

                // List meals by type (breakfast, lunch, etc.)
                items(mealTypes) { meal ->
                    val displayName = meal.name.lowercase().replaceFirstChar { it.uppercase() }
                    val expanded = expandedStates[displayName] ?: true
                    var noteText by remember { mutableStateOf(mealNotes[displayName] ?: "") }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Meal header row with expand and add buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { expandedStates[displayName] = !expanded }
                            ) {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = "Toggle Expand",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    navController.navigate("add_food/${meal.label}/${selectedDate}")
                                }) { Text("Add") }
                            }

                            // If expanded, show food items and notes
                            AnimatedVisibility(visible = expanded) {
                                Column {
                                    val items = loggedMeals[displayName] ?: listOf()
                                    if (items.isEmpty()) {
                                        Text("No foods logged yet", style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        items.forEach { item ->
                                            var expandedMenu by remember { mutableStateOf(false) }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            ) {
                                                Text("- ${item.name} x${item.quantity}", modifier = Modifier.weight(1f))
                                                Box {
                                                    IconButton(onClick = { expandedMenu = !expandedMenu }) {
                                                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                                    }
                                                    DropdownMenu(
                                                        expanded = expandedMenu,
                                                        onDismissRequest = { expandedMenu = false }
                                                    ) {
                                                        // Increase quantity
                                                        DropdownMenuItem(
                                                            text = { Text("➕") },
                                                            onClick = {
                                                                if (uid != null) {
                                                                    val newQty = item.quantity + 1
                                                                    val updated = mapOf(
                                                                        "quantity" to newQty,
                                                                        "calories" to item.calories / item.quantity * newQty,
                                                                        "protein" to item.protein / item.quantity * newQty,
                                                                        "carbs" to item.carbs / item.quantity * newQty,
                                                                        "fat" to item.fat / item.quantity * newQty
                                                                    )
                                                                    db.collection("users").document(uid)
                                                                        .collection("meal_logs").document(selectedDate.toString())
                                                                        .collection(meal.label).document(item.docId)
                                                                        .update(updated)
                                                                        .addOnSuccessListener {
                                                                            updateSummary(uid, selectedDate.toString())
                                                                        }
                                                                }
                                                            }
                                                        )
                                                        // Decrease quantity
                                                        DropdownMenuItem(
                                                            text = { Text("➖") },
                                                            onClick = {
                                                                if (uid != null && item.quantity > 1) {
                                                                    val newQty = item.quantity - 1
                                                                    val updated = mapOf(
                                                                        "quantity" to newQty,
                                                                        "calories" to item.calories / item.quantity * newQty,
                                                                        "protein" to item.protein / item.quantity * newQty,
                                                                        "carbs" to item.carbs / item.quantity * newQty,
                                                                        "fat" to item.fat / item.quantity * newQty
                                                                    )
                                                                    db.collection("users").document(uid)
                                                                        .collection("meal_logs").document(selectedDate.toString())
                                                                        .collection(meal.label).document(item.docId)
                                                                        .update(updated)
                                                                        .addOnSuccessListener {
                                                                            updateSummary(uid, selectedDate.toString())
                                                                        }
                                                                }
                                                            }
                                                        )
                                                        // Delete item
                                                        DropdownMenuItem(
                                                            text = { Text("🗑️") },
                                                            onClick = {
                                                                if (uid != null) {
                                                                    db.collection("users").document(uid)
                                                                        .collection("meal_logs").document(selectedDate.toString())
                                                                        .collection(meal.label).document(item.docId)
                                                                        .delete()
                                                                        .addOnSuccessListener {
                                                                            updateSummary(uid, selectedDate.toString())
                                                                        }
                                                                        .addOnFailureListener {
                                                                            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                }
                                                                expandedMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Note section toggle
                                    var showNote by remember { mutableStateOf(false) }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable { showNote = !showNote }
                                    ) {
                                        Icon(
                                            imageVector = if (showNote) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                            contentDescription = "Toggle Note",
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text("Meal Note", style = MaterialTheme.typography.titleMedium)
                                    }

                                    // Display and save meal note
                                    AnimatedVisibility(visible = showNote) {
                                        Column {
                                            OutlinedTextField(
                                                value = noteText,
                                                onValueChange = { noteText = it },
                                                label = { Text("Enter note") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Button(
                                                onClick = {
                                                    if (uid != null) {
                                                        db.collection("users").document(uid)
                                                            .collection("meal_logs").document(selectedDate.toString())
                                                            .set(mapOf("${meal.label}_note" to noteText), SetOptions.merge())
                                                            .addOnSuccessListener {
                                                                Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                                                            }
                                                            .addOnFailureListener {
                                                                Toast.makeText(context, "Failed to save note", Toast.LENGTH_SHORT).show()
                                                            }
                                                    }
                                                },
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text("Save Note")
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
    }
}
