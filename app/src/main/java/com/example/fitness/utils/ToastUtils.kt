package com.example.fitness.utils

import android.content.Context
import android.widget.Toast

/**
 * Utility function to display a short Toast message.
 *
 * @param context The context in which the Toast should be shown.
 * @param msg The message string to display in the Toast.
 *
 * Example usage:
 *     showToast(context, "Profile saved successfully")
 */
fun showToast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
