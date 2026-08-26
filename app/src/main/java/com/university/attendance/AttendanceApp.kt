package com.university.attendance

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

/**
 * IMPORTANT: register this in AndroidManifest.xml on the <application> tag:
 *
 *   <application
 *       android:name=".AttendanceApp"
 *       ... your existing attributes ...>
 *
 * If you already have a custom Application class, just add the
 * ThemePreferences.applySavedTheme(this) + registerActivityLifecycleCallbacks
 * block into its onCreate() instead of using this file.
 */
class AttendanceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreferences.applySavedTheme(this)

        // Forces the status bar color + icon color (dark icons on light
        // background, light icons on dark background) on EVERY Activity,
        // right when it's created. This is what makes the status bar
        // actually follow the Dark/Light switch everywhere in the app,
        // instead of depending on whatever the OS/theme happened to be
        // showing before.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is AppCompatActivity) {
                    applyStatusBarTheme(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun applyStatusBarTheme(activity: Activity) {
        val isDark = ThemePreferences.isDarkMode(activity)
        val window = activity.window

        window.statusBarColor = ContextCompat.getColor(activity, R.color.backgroundColor)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Light status bar (dark icons) when we're in LIGHT theme;
        // dark status bar (light/white icons) when in DARK theme.
        insetsController.isAppearanceLightStatusBars = !isDark
    }
}