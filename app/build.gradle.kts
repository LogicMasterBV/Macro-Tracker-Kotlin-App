plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.fitness"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fitness"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "USDA_API_KEY", "\"${project.properties["USDA_API_KEY"]}\"")
        buildConfigField("String", "GOOGLE_VISION_API", "\"${project.properties["GOOGLE_VISION_API"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    // Firebase
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.bom)

    // Credential Manager (Latest versions only)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.androidx.material.icons.extended)
    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.bom.v20231001)
    implementation(libs.androidx.compose.material3.material3)





    implementation(libs.androidx.runtime)


    // Compose UI
    implementation(libs.androidx.foundation)
    implementation(libs.filament.android)
    implementation(libs.compose)
    implementation(libs.core)
    implementation(libs.mpandroidchart)


    //camera
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)
    implementation (libs.androidx.camera.extensions)
    implementation (libs.ui)
    implementation (libs.material3)
    implementation (libs.firebase.storage.ktx)
    implementation(libs.androidx.camera.core)
    implementation (libs.guava)

    //Google vision api
    implementation (libs.okhttp)
    implementation (libs.okio)
    implementation (libs.gson)
    implementation(libs.androidx.espresso.core)
    implementation(libs.play.services.base)
    implementation(libs.play.services.base)
    implementation(libs.play.services.base)


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

