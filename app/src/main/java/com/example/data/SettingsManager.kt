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
        get() = prefs.getString(KEY_ICON_LETTER_AR, "د") ?: "د"
        set(value) = prefs.edit().putString(KEY_ICON_LETTER_AR, value).apply()

    var iconLetterEn: String
        get() = prefs.getString(KEY_ICON_LETTER_EN, "D") ?: "D"
        set(value) = prefs.edit().putString(KEY_ICON_LETTER_EN, value).apply()

    var footerText: String
        get() = prefs.getString(KEY_FOOTER_TEXT, "MAW 777644670") ?: "MAW 777644670"
        set(value) = prefs.edit().putString(KEY_FOOTER_TEXT, value).apply()

    var defaultLanguage: String
        get() = prefs.getString(KEY_DEFAULT_LANGUAGE, "ar") ?: "ar"
        set(value) = prefs.edit().putString(KEY_DEFAULT_LANGUAGE, value).apply()

    var currentUser: String?
        get() = prefs.getString(KEY_CURRENT_USER, null)
        set(value) = prefs.edit().putString(KEY_CURRENT_USER, value).apply()
}
