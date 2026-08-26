package com.university.attendance

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Single source of truth for the app's Dark/Light theme choice.
 *
 * - Persists the choice in SharedPreferences so it survives app restarts.
 * - Actually applies it via AppCompatDelegate.setDefaultNightMode(), which
 *   is what makes Android pick between res/values/colors.xml (light) and
 *   res/values-night/colors.xml (dark) automatically -- as long as both
 *   files define the SAME color names (backgroundColor, textPrimary, etc.)
 *   with different values.
 *
 * Defaults to Dark, since that's the only theme the app supported before.
 */
object ThemePreferences {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()

        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /**
     * Call once, as early as possible -- from Application.onCreate() --
     * so the saved theme is applied BEFORE any Activity inflates its
     * layout. If this only runs inside an Activity, you'd see a flash of
     * the wrong theme on cold start.
     */
    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode(context)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}