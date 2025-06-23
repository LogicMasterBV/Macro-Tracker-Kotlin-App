package com.example.fitness.ui

import android.content.Context
import android.os.CancellationSignal
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavController
import com.example.fitness.R
import com.example.fitness.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { Firebase.firestore }
    val credentialManager = remember { CredentialManager.create(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    /**
     * Creates a user profile in Firestore if it doesn't already exist.
     */
    fun createUserProfileIfNotExists(uid: String, name: String?, email: String?) {
        val usersRef = db.collection("users").document(uid)
        usersRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val profile = UserProfile(
                    uid = uid,
                    name = name ?: "",
                    surname = "",
                    age = 0,
                    weight = 0f,
                    height = 0f,
                    email = email ?: ""
                )
                usersRef.set(profile)
                    .addOnSuccessListener { showToast(context, "Google profile created") }
                    .addOnFailureListener { showToast(context, "Failed to create profile: ${it.message}") }
            }

            // Navigate to Home and clear backstack
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Login UI layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Email input field
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })

        Spacer(modifier = Modifier.height(8.dp))

        // Password input field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Login button using Firebase Auth
        Button(onClick = {
            if (email.isBlank() || password.isBlank()) {
                showToast(context, "Please enter email and password")
                return@Button
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                    val userEmail = auth.currentUser?.email
                    val name = auth.currentUser?.displayName
                    createUserProfileIfNotExists(uid, name, userEmail)
                }
                .addOnFailureListener { exception ->
                    val message = exception.message.orEmpty()
                    val errorMessage = when {
                        "password is invalid" in message -> "Incorrect password."
                        "no user record" in message -> "No account found with this email."
                        "badly formatted" in message -> "Please enter a valid email address."
                        else -> "Login failed: $message"
                    }
                    showToast(context, errorMessage)
                    Log.e("LoginScreen", "Login error: $message", exception)
                }
        }) {
            Text("Login")
        }

        // Navigate to Forgot Password screen
        Text(
            "Forgot Password?",
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable {
                    navController.navigate("forgot_password")
                },
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Google Sign-In button using Credential Manager API
        OutlinedButton(onClick = {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest(listOf(googleIdOption))

            credentialManager.getCredentialAsync(
                context,
                request,
                CancellationSignal(),
                context.mainExecutor,
                object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                    override fun onResult(result: GetCredentialResponse) {
                        try {
                            // Extract Google credential
                            val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
                            val idToken = googleCred.idToken
                            val emailFromCred = googleCred.id
                            val displayName = googleCred.displayName

                            // Convert to Firebase credential
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                            // Sign in with Firebase
                            auth.signInWithCredential(firebaseCredential)
                                .addOnSuccessListener {
                                    val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                                    createUserProfileIfNotExists(uid, displayName, emailFromCred)
                                }
                                .addOnFailureListener {
                                    showToast(context, "Google Sign-In failed: ${it.message}")
                                }

                        } catch (e: Exception) {
                            showToast(context, "Parsing failed: ${e.message}")
                        }
                    }

                    override fun onError(e: GetCredentialException) {
                        showToast(context, "Google sign-in failed: ${e.message}")
                    }
                }
            )
        }) {
            Text("Sign in with Google")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigate to Register screen
        TextButton(onClick = { navController.navigate("register") }) {
            Text("Don't have an account? Register")
        }
    }
}

/**
 * Displays a short toast message.
 */
fun showToast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
