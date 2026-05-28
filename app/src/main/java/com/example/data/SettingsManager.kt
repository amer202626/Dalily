package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dalili_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "language"
        private const val KEY_APP_NAME_AR = "app_name_ar"
        private const val KEY_APP_NAME_EN = "app_name_en"
        private const val KEY_PRIMARY_COLOR = "primary_color"
        private const val KEY_SECONDARY_COLOR = "secondary_color"
        private const val KEY_ICON_LETTER_AR = "icon_letter_ar"
        private const val KEY_ICON_LETTER_EN = "icon_letter_en"
        private const val KEY_FOOTER_TEXT = "footer_text"
        private const val KEY_DEFAULT_LANGUAGE = "default_language"
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_WELCOME_MESSAGE_AR = "welcome_message_ar"
        private const val KEY_WELCOME_MESSAGE_EN = "welcome_message_en"
        private const val KEY_SHOW_WELCOME_MSG_INSTEAD_OF_LOGO = "show_welcome_msg_instead_logo"
        private const val KEY_CUSTOM_WELCOME_LOGO_URL = "custom_welcome_logo_url"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, null) ?: defaultLanguage
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var appNameAr: String
        get() = prefs.getString(KEY_APP_NAME_AR, "دليلي") ?: "دليلي"
        set(value) = prefs.edit().putString(KEY_APP_NAME_AR, value).apply()

    var appNameEn: String
        get() = prefs.getString(KEY_APP_NAME_EN, "Dalili") ?: "Dalili"
        set(value) = prefs.edit().putString(KEY_APP_NAME_EN, value).apply()

    var primaryColor: String
        get() = prefs.getString(KEY_PRIMARY_COLOR, "#1E88E5") ?: "#1E88E5"
        set(value) = prefs.edit().putString(KEY_PRIMARY_COLOR, value).apply()

    var secondaryColor: String
        get() = prefs.getString(KEY_SECONDARY_COLOR, "#FB8C00") ?: "#FB8C00"
        set(value) = prefs.edit().putString(KEY_SECONDARY_COLOR, value).apply()

    var iconLetterAr: String
        get() = prefs.getString(KEY_ICON_LETTER_AR, "خدمات") ?: "خدمات"
        set(value) = prefs.edit().putString(KEY_ICON_LETTER_AR, value).apply()

    var iconLetterEn: String
        get() = prefs.getString(KEY_ICON_LETTER_EN, "Services") ?: "Services"
        set(value) = prefs.edit().putString(KEY_ICON_LETTER_EN, value).apply()

    var footerText: String
        get() = prefs.getString(KEY_FOOTER_TEXT, "MAW 777644670") ?: "MAW 777644670"
        set(value) = prefs.edit().putString(KEY_FOOTER_TEXT, value).apply()

    var defaultLanguage: String
        get() = prefs.getString(KEY_DEFAULT_LANGUAGE, "ar") ?: "ar"
        set(value) = prefs.edit().putString(KEY_DEFAULT_LANGUAGE, value).apply()

    var welcomeMessageAr: String
        get() = prefs.getString(KEY_WELCOME_MESSAGE_AR, "كل الخدمات في تطبيق واحد") ?: "كل الخدمات في تطبيق واحد"
        set(value) = prefs.edit().putString(KEY_WELCOME_MESSAGE_AR, value).apply()

    var welcomeMessageEn: String
        get() = prefs.getString(KEY_WELCOME_MESSAGE_EN, "All services in one app") ?: "All services in one app"
        set(value) = prefs.edit().putString(KEY_WELCOME_MESSAGE_EN, value).apply()

    var showWelcomeMessageInsteadOfLogo: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WELCOME_MSG_INSTEAD_OF_LOGO, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WELCOME_MSG_INSTEAD_OF_LOGO, value).apply()

    var customWelcomeLogoUrl: String
        get() = prefs.getString(KEY_CUSTOM_WELCOME_LOGO_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_WELCOME_LOGO_URL, value).apply()

    var geminiApiKeySetting: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var currentUser: String?
        get() = prefs.getString(KEY_CURRENT_USER, null)
        set(value) = prefs.edit().putString(KEY_CURRENT_USER, value).apply()

    var adminPasswordOverride: String?
        get() = prefs.getString("admin_password_override", null)
        set(value) = prefs.edit().putString("admin_password_override", value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, value).apply()

    var lastSyncTimeIso: String
        get() = prefs.getString("last_sync_time_iso", "1970-01-01T00:00:00.000Z") ?: "1970-01-01T00:00:00.000Z"
        set(value) = prefs.edit().putString("last_sync_time_iso", value).apply()

    fun saveAdminsLocal(admins: List<Admin>) {
        val encoded = admins.joinToString(";") { "${it.id}||${it.username}||${it.passwordHash}||${it.role}||${it.isActive}" }
        prefs.edit().putString("cached_admins_list", encoded).apply()
    }

    fun getAdminsLocal(): List<Admin> {
        val encoded = prefs.getString("cached_admins_list", null) ?: return emptyList()
        if (encoded.isBlank()) return emptyList()
        return encoded.split(";").mapNotNull { block ->
            val parts = block.split("||")
            if (parts.size >= 5) {
                Admin(
                    id = parts[0],
                    username = parts[1],
                    passwordHash = parts[2],
                    role = parts[3],
                    isActive = parts[4].toBoolean()
                )
            } else null
        }
    }
}
