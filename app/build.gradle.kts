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

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

    implementation("com.google.firebase:firebase-auth")

    implementation("com.google.firebase:firebase-firestore")

    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-storage")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // QR Code generation (Teacher side — session QR banane ke liye)
    implementation("com.google.zxing:core:3.5.3")

    // RecyclerView, ViewPager2 and CardView are used directly in Kotlin source
    // (imports: androidx.recyclerview.widget.*, androidx.viewpager2.widget.ViewPager2)
    // across ~38 files (adapters, onboarding, dashboards). The Material library only
    // exposes these transitively at "runtime" scope, not on the compile classpath, so
    // without declaring them explicitly here every one of those files fails to compile
    // with "unresolved reference". This was the root cause of the build errors.
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // Coroutines <-> Firebase Task bridge (needed for `.await()` used across repositories)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
}