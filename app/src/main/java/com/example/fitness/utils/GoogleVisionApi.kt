package com.example.fitness.utils

import android.graphics.Bitmap
import android.util.Base64
import com.example.fitness.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Converts a Bitmap image into a Base64-encoded JPEG string.
 *
 * This format is required by the Google Vision API when sending image data in JSON.
 */
fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream) // Compress as JPEG with 90% quality
    val byteArray = stream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP) // Base64 encode with no line breaks
}

/**
 * Sends an image to the Google Vision API for label detection and returns the result asynchronously.
 *
 * @param bitmap The image to analyze.
 * @return JSONArray? containing labelAnnotations from the API response.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun analyzeImageWithVisionApi(bitmap: Bitmap): JSONArray? {
    val apiKey = BuildConfig.GOOGLE_VISION_API // Read from secure BuildConfig field
    val base64Image = bitmapToBase64(bitmap)

    // Construct the JSON body for the Vision API request
    val jsonBody = JSONObject().apply {
        put("requests", JSONArray().apply {
            put(JSONObject().apply {
                put("image", JSONObject().apply {
                    put("content", base64Image)
                })
                put("features", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "LABEL_DETECTION")
                        put("maxResults", 10) // Limit to 10 labels
                    })
                })
            })
        })
    }

    // Prepare the HTTP request body
    val requestBody = jsonBody.toString()
        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

    // Build the HTTP POST request
    val request = Request.Builder()
        .url("https://vision.googleapis.com/v1/images:annotate?key=$apiKey")
        .post(requestBody)
        .build()

    val client = OkHttpClient()

    // Make the request asynchronously with suspendCancellableCoroutine
    return suspendCancellableCoroutine { cont ->
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resume(null, null) // Resume coroutine with null if request fails
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        cont.resume(null, null) // Resume coroutine with null if HTTP error
                        return
                    }

                    val responseString = it.body?.string() ?: ""
                    val json = JSONObject(responseString)

                    // Print formatted JSON for debugging
                    println("GOOGLE VISION RESPONSE:\n" + json.toString(2))

                    val labelAnnotations = json
                        .optJSONArray("responses")
                        ?.optJSONObject(0)
                        ?.optJSONArray("labelAnnotations")

                    cont.resume(labelAnnotations, null) // Resume with parsed result
                }
            }
        })
    }
}

/**
 * Parses label annotations into a list of description strings.
 *
 * @param labelAnnotations A JSONArray of labels from the Vision API.
 * @return A list of label descriptions, or "No result" if null.
 */
fun parseLabels(labelAnnotations: JSONArray?): List<String> {
    if (labelAnnotations == null) return listOf("No result")

    val labels = mutableListOf<String>()
    for (i in 0 until labelAnnotations.length()) {
        val label = labelAnnotations.getJSONObject(i).getString("description")
        labels.add(label)
    }
    return labels
}
