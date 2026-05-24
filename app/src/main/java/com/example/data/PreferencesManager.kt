package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pricewala_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SHOP_NAME = "shop_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_LANGUAGE = "language" // "en" or "ur"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_PW = "saved_pw"
    }

    var shopName: String
        get() = prefs.getString(KEY_SHOP_NAME, "PriceWala Store") ?: "PriceWala Store"
        set(value) = prefs.edit().putString(KEY_SHOP_NAME, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var voiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var rememberMe: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_ME, false)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_ME, value).apply()

    var savedPassword: String
        get() = prefs.getString(KEY_SAVED_PW, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_PW, value).apply()

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }
}
