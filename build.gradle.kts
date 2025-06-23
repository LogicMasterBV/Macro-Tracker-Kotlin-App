// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // Android application plugin (used in :app module)
    alias(libs.plugins.android.application) apply false

    // Kotlin Android plugin (adds Kotlin support for Android development)
    alias(libs.plugins.kotlin.android) apply false

    // Jetpack Compose plugin (enables Compose compiler features)
    alias(libs.plugins.kotlin.compose) apply false

    // Google Services plugin (for Firebase Authentication, Firestore, Analytics, etc.)
    alias(libs.plugins.google.gms.google.services) apply false
}
