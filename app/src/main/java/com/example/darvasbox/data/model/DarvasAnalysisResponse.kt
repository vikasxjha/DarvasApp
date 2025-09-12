package com.example.darvasbox.data.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DarvasAnalysisResponse(
    val symbol: String,
    val currentPrice: Double,
    val boxHigh: Double?,
    val boxLow: Double?,
    val signal: SignalType,
    val volume: Long,
    val change: Double,
    val changePercent: Double,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val avgVolume: Long? = null,
    val volumeRatio: Double? = null,
    val confidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val marketCap: Double? = null,
    val sector: String? = null,
    val success: Boolean = true,
    val errorMessage: String? = null
) {

    val formattedTimestamp: String
        get() = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    val isValidSignal: Boolean
        get() = success && boxHigh != null && boxLow != null

    val volumeConfirmation: Boolean
        get() = volumeRatio?.let { it > 1.2 } ?: false

    fun getSignalStrength(): String = when {
        !isValidSignal -> "Invalid"
        volumeConfirmation && confidence == ConfidenceLevel.HIGH -> "Strong"
        volumeConfirmation -> "Moderate"
        else -> "Weak"
    }

    /**
     * Legacy compatibility method for existing code
     */
    fun getSignalString(): String = signal.displayName
}

enum class SignalType(val displayName: String) {
    BUY("BUY"),
    SELL("SELL"),
    IGNORE("IGNORE"),
    HOLD("HOLD"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromString(value: String): SignalType {
            return values().find { it.displayName.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class ConfidenceLevel {
    LOW, MEDIUM, HIGH
}
