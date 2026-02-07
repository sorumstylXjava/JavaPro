package com.javapro.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("javapro_settings", Context.MODE_PRIVATE)

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val darkModeFlow: StateFlow<Boolean> = _darkMode

    private val _language = MutableStateFlow(prefs.getString("lang", "en") ?: "en")
    val languageFlow: StateFlow<String> = _language

    private val _bootApply = MutableStateFlow(prefs.getBoolean("boot_apply", false))
    val bootApplyFlow: StateFlow<Boolean> = _bootApply
    
    private val _fpsEnabled = MutableStateFlow(prefs.getBoolean("fps_enabled", false))
    val fpsEnabledFlow: StateFlow<Boolean> = _fpsEnabled

    private val _scaleVal = MutableStateFlow(prefs.getFloat("scale_val", 1.0f))
    val scaleValFlow: StateFlow<Float> = _scaleVal

    private val _redVal = MutableStateFlow(prefs.getFloat("red_val", 1000f)) 
    val redValFlow: StateFlow<Float> = _redVal

    private val _greenVal = MutableStateFlow(prefs.getFloat("green_val", 1000f))
    val greenValFlow: StateFlow<Float> = _greenVal

    private val _blueVal = MutableStateFlow(prefs.getFloat("blue_val", 1000f))
    val blueValFlow: StateFlow<Float> = _blueVal
    
    private val _satVal = MutableStateFlow(prefs.getFloat("sat_val", 1000f))
    val satValFlow: StateFlow<Float> = _satVal

    private val _resConfirmed = MutableStateFlow(prefs.getBoolean("res_confirmed", false))
    val resConfirmedFlow: StateFlow<Boolean> = _resConfirmed

    // --- FUNGSI SETTER ---

    fun setFpsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("fps_enabled", enabled).apply()
        _fpsEnabled.value = enabled
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _darkMode.value = enabled
    }

    // Fungsi ini tadi hilang, sekarang sudah ada kembali
    fun setLanguage(lang: String) {
        prefs.edit().putString("lang", lang).apply()
        _language.value = lang
    }

    fun setBootApply(enabled: Boolean) {
        prefs.edit().putBoolean("boot_apply", enabled).apply()
        _bootApply.value = enabled
    }

    fun setScale(value: Float) {
        prefs.edit().putFloat("scale_val", value).apply()
        _scaleVal.value = value
    }
    
    fun setResConfirmed(confirmed: Boolean) {
        prefs.edit().putBoolean("res_confirmed", confirmed).apply()
        _resConfirmed.value = confirmed
    }

    fun setRGB(r: Float, g: Float, b: Float) {
        prefs.edit()
            .putFloat("red_val", r)
            .putFloat("green_val", g)
            .putFloat("blue_val", b)
            .apply()
        _redVal.value = r
        _greenVal.value = g
        _blueVal.value = b
    }
    
    fun setSat(sat: Float) {
        prefs.edit().putFloat("sat_val", sat).apply()
        _satVal.value = sat
    }
}
