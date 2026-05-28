package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dalili_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_CACHED_ADMINS = "cached_admins_list"
    }

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, value).apply()

    var lastSyncTimeIso: String
        get() = prefs.getString("last_sync_time_iso", "1970-01-01T00:00:00.000Z") ?: "1970-01-01T00:00:00.000Z"
        set(value) = prefs.edit().putString("last_sync_time_iso", value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "ar") ?: "ar"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun saveAdminsLocal(admins: List<Admin>) {
        val encoded = admins.joinToString(";") { "${it.username}||${it.passwordHash}||${it.role}||${it.isActive}" }
        prefs.edit().putString(KEY_CACHED_ADMINS, encoded).apply()
    }

    fun getAdminsLocal(): List<Admin> {
        val encoded = prefs.getString(KEY_CACHED_ADMINS, "") ?: ""
        if (encoded.isBlank()) return emptyList()
        return try {
            encoded.split(";").map {
                val parts = it.split("||")
                Admin(
                    username = parts[0],
                    passwordHash = parts[1],
                    role = parts[2],
                    isActive = parts[3].toBoolean()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
