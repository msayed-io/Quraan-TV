package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // =========================================================================
    // 1. FAVORITES MANAGEMENT
    // =========================================================================
    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun isFavorite(filePath: String): Boolean {
        return getFavorites().contains(filePath)
    }

    fun toggleFavorite(filePath: String): Boolean {
        val current = getFavorites().toMutableSet()
        val isFavNow = if (current.contains(filePath)) {
            current.remove(filePath)
            false
        } else {
            current.add(filePath)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return isFavNow
    }

    // =========================================================================
    // 2. AUTO-RESUME / BOOKMARK MANAGEMENT
    // =========================================================================
    fun saveBookmark(filePath: String, positionMs: Long) {
        if (filePath.isBlank()) return
        prefs.edit()
            .putString(KEY_BOOKMARK_FILE, filePath)
            .putLong(KEY_BOOKMARK_POS, positionMs)
            .apply()
    }

    fun getBookmark(): Pair<String, Long>? {
        val path = prefs.getString(KEY_BOOKMARK_FILE, null) ?: return null
        val pos = prefs.getLong(KEY_BOOKMARK_POS, 0L)
        return Pair(path, pos)
    }

    fun clearBookmark() {
        prefs.edit()
            .remove(KEY_BOOKMARK_FILE)
            .remove(KEY_BOOKMARK_POS)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "quran_tv_prefs"
        private const val KEY_FAVORITES = "key_favorites"
        private const val KEY_BOOKMARK_FILE = "key_bookmark_file"
        private const val KEY_BOOKMARK_POS = "key_bookmark_pos"
    }
}
