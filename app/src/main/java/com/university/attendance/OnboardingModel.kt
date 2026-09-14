package com.university.attendance

data class OnboardingModel(
    val image: Int,
    val title: String,
    val desc: String,
    val badge: String = ""  // New field for the label
)