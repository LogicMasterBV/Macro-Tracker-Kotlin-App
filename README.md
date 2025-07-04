# Calz: Personal Macro & Fitness Tracker

**Calz** is an Android mobile application developed as part of the final-year module for the BSc (Hons) Computer Science degree at London Metropolitan University. Built using **Kotlin** with **Jetpack Compose**, Calz offers a streamlined experience for users to track their nutrition, monitor weight, and reflect on personal health progress.

[![Watch the demo](https://img.youtube.com/vi/RamcF7L_bzg/0.jpg)](https://www.youtube.com/watch?v=RamcF7L_bzg)

## 📱 Features

- 🔐 **Firebase Authentication** (Email/Password + Google Sign-In)
- 🍽️ **Daily Meal Logging** across Breakfast, Lunch, Dinner, and Snacks
- 🔢 **Calorie & Macronutrient Tracking** (Protein, Carbs, Fat)
- 📷 **Image-Based Food Detection** via Google Vision API
- 🔎 **USDA FoodData Central API** integration for real-time food search
- 📈 **Weight & BMI Progress Tracking** with charts
- 🧑 **User Profile Customization** (Macro targets, weight, height)
- 🔄 **Real-Time Sync** using Firebase Firestore

## 🧠 Why Calz?

While apps like CalAi and Cronometer offer advanced features, they can be cluttered or ad-heavy. **Calz** focuses on simplicity, privacy, and practical functionality — making it ideal for users who want a no-frills nutrition tracker that works seamlessly across devices.

## 🏗️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM with Coroutines & StateFlow
- **Backend**: Firebase (Auth + Firestore)
- **APIs**:
  - Google Vision API (image-based food recognition)
  - USDA FoodData Central API (nutrition database)


## 🧪 Testing & Deployment

Tested on:
- Android Emulator (API 33)
- Samsung Galaxy S21 (Android 13)

Manually tested flows:
- Authentication (Email & Google)
- Meal logging and macro updates
- Image capture and Vision API integration
- USDA search and food item parsing
- Weight input and BMI calculation
- Firebase sync & offline behavior
