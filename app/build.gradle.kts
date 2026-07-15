plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.university.attendance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.university.attendance"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // QR Code generation (Teacher side — session QR banane ke liye)
    implementation("com.google.zxing:core:3.5.3")

    // QR Code scanning (Student side — camera se scan karne ke liye)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")


//
//   </-- # Add these to app/build.gradle.kts (module-level), inside dependencies { } block
//    # ----------------------------------------------------------------------------
//    # Skip any line below if you already have that exact dependency (avoid duplicates,
//    # since duplicate declarations were the earlier symbol-resolution issue you had). -->

    //1. Coroutines <-> Firebase Task bridge (needed for `.await()` used in StudentRepository)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // 2. ViewModel + LiveData (needed for AddStudentViewModel)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

   // # 3. Firestore (skip if already present via your Firebase BOM)

//    # ----------------------------------------------------------------------------
//    # HOW TO CHECK IF YOU ALREADY HAVE THESE:
//    # Open app/build.gradle.kts -> look inside dependencies { ... } for lines
//    # containing "lifecycle-viewmodel", "coroutines-play-services", or "firestore".
//    # If found, don't add it again -- that exact duplication is what caused your
//    # earlier IDE symbol resolution errors.
}