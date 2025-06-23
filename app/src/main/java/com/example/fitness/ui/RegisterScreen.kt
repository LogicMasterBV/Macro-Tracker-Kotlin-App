package com.example.fitness.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitness.model.UserProfile
import com.example.fitness.utils.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * A screen where users can register with Firebase Authentication
 * and save a detailed UserProfile to Firestore.
 */
@Composable
fun RegisterScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { Firebase.firestore }

    // Form field states
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Register", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // User input fields
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        OutlinedTextField(value = surname, onValueChange = { surname = it }, label = { Text("Surname") })
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") })
        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") })
        OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Registration button logic
        Button(onClick = {
            // Basic validation
            if (name.isBlank() || surname.isBlank() || age.isBlank() || weight.isBlank() ||
                height.isBlank() || email.isBlank() || password.isBlank()
            ) {
                showToast(context, "Please fill in all fields")
                return@Button
            }

            // Firebase Auth: create new user
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

                    // Build user profile model
                    val profile = UserProfile(
                        uid = uid,
                        name = name,
                        surname = surname,
                        age = age.toIntOrNull() ?: 0,
                        weight = weight.toFloatOrNull() ?: 0f,
                        height = height.toFloatOrNull() ?: 0f,
                        email = email
                    )

                    // Save to Firestore
                    db.collection("users").document(uid).set(profile)
                        .addOnSuccessListener {
                            showToast(context, "Registration successful")
                            navController.navigate("home") {
                                popUpTo("register") { inclusive = true } // Clear backstack
                            }
                        }
                        .addOnFailureListener {
                            showToast(context, "Failed to save profile: ${it.message}")
                        }
                }
                .addOnFailureListener {
                    showToast(context, "Registration failed: ${it.message}")
                }
        }) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigate to Login screen if user already has an account
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Login")
        }
    }
}
