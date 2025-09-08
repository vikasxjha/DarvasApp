package com.example.darvasbox.data.model

data class SheetsAnalysisResult(
    val symbols: List<String>,
    val timestamp: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val analysisResults: List<StockAnalysisResult> = emptyList()
)

data class StockAnalysisResult(
    val symbol: String,
    val currentPrice: Double?,
    val signal: String,
    val boxHigh: Double?,
    val boxLow: Double?,
    val volume: Long?,
    val analysisSuccess: Boolean,
    val errorMessage: String? = null
)
