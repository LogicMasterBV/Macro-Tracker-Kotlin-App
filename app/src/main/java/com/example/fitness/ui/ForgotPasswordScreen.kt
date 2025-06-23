package com.example.fitness.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance() // Firebase authentication instance

    var email by remember { mutableStateOf("") }       // Email input state
    var isLoading by remember { mutableStateOf(false) } // Loading flag for feedback and disabling button

    // Main layout column centered vertically and horizontally
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen title
        Text("Reset Password", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        // Email input field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Send password reset link button
        Button(
            onClick = {
                if (email.isBlank()) {
                    // Prompt user to enter email
                    Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                // Send Firebase password reset email
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                        navController.popBackStack() // Return to login screen
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener {
                        isLoading = false
                    }
            },
            enabled = !isLoading // Disable while loading to prevent multiple taps
        ) {
            Text("Send Reset Link")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Back to login navigation
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Back to Login")
        }
    }
}
