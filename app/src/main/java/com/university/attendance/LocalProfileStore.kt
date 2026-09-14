package com.university.attendance

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Handles LOCAL (on-device) storage of the admin's edited profile info:
 * display name and photo. This is intentionally separate from Firebase --
 * per current requirements, profile edits are saved locally (SharedPreferences
 * for the name, internal app storage for the photo file), not synced to
 * any backend.
 *
 * Photos can't be stored directly in SharedPreferences (it only holds
 * primitives/strings) -- so the picked image is copied into the app's
 * private internal storage directory, and only its file PATH is saved in
 * SharedPreferences. This is the standard Android pattern for "remember
 * a user-picked image across app restarts" without needing any backend.
 */
object LocalProfileStore {

    private const val PREFS_NAME = "admin_profile_prefs"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_PHOTO_PATH = "photo_path"
    private const val PHOTO_FILENAME = "admin_profile_photo.jpg"

    fun saveDisplayName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim()).apply()
    }

    fun getDisplayName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISPLAY_NAME, null)
    }

    /**
     * Copies the picked image (from a content:// Uri, e.g. from a gallery
     * picker) into the app's private internal storage, and saves that
     * file's path in SharedPreferences. Returns the saved file's path, or
     * null if the copy failed.
     */
    fun savePhoto(context: Context, sourceUri: Uri): String? {
        return try {
            val destFile = File(context.filesDir, PHOTO_FILENAME)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PHOTO_PATH, destFile.absolutePath).apply()
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Returns the saved photo's local file path, or null if none has been set. */
    fun getPhotoPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_PHOTO_PATH, null)
        return if (path != null && File(path).exists()) path else null
    }
}