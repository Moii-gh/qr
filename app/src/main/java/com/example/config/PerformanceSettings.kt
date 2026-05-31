package com.example.config

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PerformanceSettings {
    private const val PREFS_NAME = "performance_prefs"
    private const val KEY_POWER_SAVING = "power_saving_mode"

    private var prefs: SharedPreferences? = null

    // Compose state for reactive UI updates
    var isPowerSavingMode by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isPowerSavingMode = prefs?.getBoolean(KEY_POWER_SAVING, false) ?: false
    }

    fun togglePowerSaving(context: Context) {
        val newValue = !isPowerSavingMode
        isPowerSavingMode = newValue
        prefs?.edit()?.putBoolean(KEY_POWER_SAVING, newValue)?.apply()
    }
}
