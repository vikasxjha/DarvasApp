package com.example.darvasbox.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AnalysisPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "darvas_box_analysis_prefs"
        private const val KEY_ANALYSIS_INTERVAL = "analysis_interval_minutes"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val DEFAULT_INTERVAL = 10L // 10 minutes default
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var analysisIntervalMinutes: Long
        get() = prefs.getLong(KEY_ANALYSIS_INTERVAL, DEFAULT_INTERVAL)
        set(value) = prefs.edit().putLong(KEY_ANALYSIS_INTERVAL, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    fun getIntervalOptions(): List<Pair<String, Long>> {
        return listOf(
            "5 minutes" to 5L,
            "10 minutes" to 10L,
            "15 minutes" to 15L,
            "30 minutes" to 30L,
            "1 hour" to 60L,
            "2 hours" to 120L
        )
    }
}
