package com.example.darvasbox.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.darvasbox.data.model.SignalType

@Entity(tableName = "stock_data")
data class StockData(
    @PrimaryKey val symbol: String,
    val price: Double,
    val boxHigh: Double,
    val boxLow: Double,
    val signal: SignalType, // BUY, SELL, IGNORE, HOLD, UNKNOWN
    val volume: Long,
    val change: Double,
    val changePercent: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Legacy compatibility method for existing code that expects String
     */
    fun getSignalString(): String = signal.displayName
}
