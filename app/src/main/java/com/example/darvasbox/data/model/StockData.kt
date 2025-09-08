package com.example.darvasbox.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "stock_data")
data class StockData(
    @PrimaryKey
    val symbol: String,
    val price: Double,
    @SerializedName("box_high")
    val boxHigh: Double,
    @SerializedName("box_low")
    val boxLow: Double,
    val signal: String, // "BUY", "SELL", "IGNORE"
    val timestamp: Long = System.currentTimeMillis(),
    val volume: Long? = null,
    val change: Double? = null,
    val changePercent: Double? = null
)

data class DarvasAnalysisResponse(
    val symbol: String,
    val price: Double,
    @SerializedName("box_high")
    val boxHigh: Double,
    @SerializedName("box_low")
    val boxLow: Double,
    val signal: String,
    val volume: Long? = null,
    val change: Double? = null,
    @SerializedName("change_percent")
    val changePercent: Double? = null
)

data class CandleData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class DarvasBox(
    val high: Double,
    val low: Double,
    val startTime: Long,
    val endTime: Long
)

enum class SignalType {
    BUY, SELL, IGNORE
}
