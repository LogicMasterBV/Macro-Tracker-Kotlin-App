package com.example.fitness.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Performs a USDA API search for a given food query.
 *
 * This function is suspendable and executes on the IO dispatcher.
 * It parses the JSON response to extract a list of [FoodItem] objects.
 *
 * @param query The food name to search for (e.g., "apple").
 * @param apiKey The USDA API key stored securely in your app config.
 * @return A list of matching food items with calorie and macro data.
 */
suspend fun searchUSDA(query: String, apiKey: String): List<FoodItem> = withContext(Dispatchers.IO) {
    // Encode the search query to be URL-safe
    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
    val url = "https://api.nal.usda.gov/fdc/v1/foods/search?query=$encoded&pageSize=10&api_key=$apiKey"

    Log.d("USDA Search", "Request URL: $url")

    try {
        // Open HTTPS connection to the USDA endpoint
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.connect()

        // Read the full response body as text
        val response = connection.inputStream.bufferedReader().readText()
        Log.d("USDA Search", "Raw Response: $response")

        val json = JSONObject(response)
        val foods = json.getJSONArray("foods")

        Log.d("USDA Search", "Foods found: ${foods.length()}")

        // Parse each food item in the response
        List(foods.length()) { i ->
            val f = foods.getJSONObject(i)
            val name = f.optString("description") // Human-readable name
            val nutrients = f.optJSONArray("foodNutrients")

            var calories = 0
            var protein = 0
            var fat = 0
            var carbs = 0

            // Loop through each nutrient to extract known macros
            if (nutrients != null) {
                for (j in 0 until nutrients.length()) {
                    val n = nutrients.getJSONObject(j)
                    val nutrientName = n.optString("nutrientName")
                    val value = n.optDouble("value", 0.0)

                    when (nutrientName) {
                        "Energy" -> calories = value.toInt()
                        "Protein" -> protein = value.toInt()
                        "Total lipid (fat)" -> fat = value.toInt()
                        "Carbohydrate, by difference" -> carbs = value.toInt()
                    }

                    Log.d("USDA Search", "[$name] $nutrientName = $value")
                }
            } else {
                Log.w("USDA Search", "No nutrients for $name")
            }

            // Create and return a FoodItem with the extracted data
            FoodItem(name, calories, protein, carbs, fat)
        }

    } catch (e: Exception) {
        Log.e("USDA Search", "Error: ${e.message}", e)
        emptyList() // Fallback in case of API failure
    }
}
