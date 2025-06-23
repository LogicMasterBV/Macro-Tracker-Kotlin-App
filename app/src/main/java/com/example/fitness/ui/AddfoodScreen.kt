package com.example.fitness.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitness.BuildConfig
import com.example.fitness.model.FoodItem
import com.example.fitness.model.searchUSDA
import com.example.fitness.utils.fetchMacroTotals
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun AddFoodScreen(
    mealType: String,            // e.g., "breakfast", "lunch"
    date: String,                // selected date for meal log
    navController: NavController,
    initialQuery: String = ""    // optional initial search query (e.g., from image detection)
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    val addedThisSession = remember { mutableStateListOf<String>() }         // recently added or updated items
    val quantityMap = remember { mutableStateMapOf<String, Int>() }         // per-item quantity state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            "Add food to ${mealType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}",
            style = MaterialTheme.typography.headlineMedium
        )

        // Search input field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Enter food name") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Show progress indicator while loading search results
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // List of search results
            LazyColumn {
                items(searchResults) { food ->
                    val quantity = quantityMap[food.name] ?: 1

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Row for food name and quantity adjustment buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    food.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                // Quantity decrease button
                                IconButton(onClick = {
                                    quantityMap[food.name] = (quantity - 1).coerceAtLeast(1)
                                }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text("$quantity", modifier = Modifier.padding(horizontal = 4.dp))
                                // Quantity increase button
                                IconButton(onClick = {
                                    quantityMap[food.name] = quantity + 1
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }

                            // Macro information scaled by quantity
                            Text("Calories: ${food.calories * quantity} kcal")
                            Text("Protein: ${food.protein * quantity} g")
                            Text("Carbs: ${food.carbs * quantity} g")
                            Text("Fat: ${food.fat * quantity} g")

                            Spacer(modifier = Modifier.height(8.dp))

                            // Button to add the food item to Firestore
                            Button(onClick = {
                                if (uid != null) {
                                    val mealRef = db.collection("users")
                                        .document(uid)
                                        .collection("meal_logs")
                                        .document(date)
                                        .collection(mealType)

                                    // Check if food already exists
                                    mealRef
                                        .whereEqualTo("name", food.name)
                                        .get()
                                        .addOnSuccessListener { query ->
                                            if (query.isEmpty) {
                                                // Add new item if not exists
                                                val foodMap = mapOf(
                                                    "name" to food.name,
                                                    "quantity" to quantity,
                                                    "calories" to food.calories * quantity,
                                                    "protein" to food.protein * quantity,
                                                    "carbs" to food.carbs * quantity,
                                                    "fat" to food.fat * quantity
                                                )
                                                mealRef.add(foodMap)
                                                    .addOnSuccessListener {
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            fetchMacroTotals(uid, date)
                                                        }
                                                        Toast.makeText(context, "Added to $mealType", Toast.LENGTH_SHORT).show()
                                                        addedThisSession.add("${food.name} x$quantity")
                                                        searchQuery = ""
                                                    }
                                                    .addOnFailureListener {
                                                        Log.e("AddFoodScreen", "Error adding", it)
                                                        Toast.makeText(context, "Failed to add", Toast.LENGTH_SHORT).show()
                                                    }
                                            } else {
                                                // Update existing item
                                                val doc = query.documents.first()
                                                val existing = doc.data!!
                                                val oldQty = (existing["quantity"] as Long).toInt()
                                                val newQty = oldQty + quantity
                                                mealRef.document(doc.id).update(
                                                    mapOf(
                                                        "quantity" to newQty,
                                                        "calories" to food.calories * newQty,
                                                        "protein" to food.protein * newQty,
                                                        "carbs" to food.carbs * newQty,
                                                        "fat" to food.fat * newQty
                                                    )
                                                ).addOnSuccessListener {
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        fetchMacroTotals(uid, date)
                                                    }
                                                    Toast.makeText(context, "Updated ${food.name}", Toast.LENGTH_SHORT).show()
                                                    addedThisSession.add("${food.name} +$quantity")
                                                    searchQuery = ""
                                                }.addOnFailureListener {
                                                    Log.e("AddFoodScreen", "Error updating", it)
                                                    Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                }
                            }) {
                                Text("Add to ${mealType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}")
                            }
                        }
                    }
                }
            }
        }

        // List of recently added/updated food items for user feedback
        if (addedThisSession.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recently Added:", style = MaterialTheme.typography.titleSmall)
            addedThisSession.forEach { item ->
                Text("• $item", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = { addedThisSession.clear() }) {
                Text("Clear List")
            }
        }
    }

    // Debounced USDA food search logic using Flows
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(300)                         // wait 300ms after typing stops
            .filter { it.isNotBlank() }            // skip if query is blank
            .distinctUntilChanged()                // skip duplicate queries
            .collectLatest { query ->
                isLoading = true
                try {
                    // API call to USDA to fetch food data
                    searchResults = searchUSDA(query, BuildConfig.USDA_API_KEY)
                } catch (e: Exception) {
                    Log.e("USDA", "API error: ${e.message}")
                    Toast.makeText(context, "Error fetching results", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
    }
}
