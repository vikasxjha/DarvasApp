package com.example.darvasbox.data.service

import com.example.darvasbox.data.model.SheetsAnalysisResult
import com.example.darvasbox.data.model.StockAnalysisResult
import com.example.darvasbox.data.network.GoogleSheetsService
import com.example.darvasbox.data.repository.SimpleStockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ManualAnalysisService(
    private val googleSheetsService: GoogleSheetsService,
    private val stockRepository: SimpleStockRepository
) {
    companion object {
        const val GOOGLE_SHEET_URL = "https://docs.google.com/spreadsheets/d/1paWhSx9l-sJBYfJBTON0zPgTcuTVqPuUSQGv5NTWgH0/edit?usp=sharing"
    }

    suspend fun performManualAnalysis(): SheetsAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // Fetch symbols from Google Sheets
            val symbolsResult = googleSheetsService.fetchSuitableSymbols(GOOGLE_SHEET_URL)

            if (symbolsResult.isFailure) {
                return@withContext SheetsAnalysisResult(
                    symbols = emptyList(),
                    timestamp = timestamp,
                    success = false,
                    errorMessage = "Failed to fetch from Google Sheets: ${symbolsResult.exceptionOrNull()?.message}"
                )
            }

            val symbols = symbolsResult.getOrNull() ?: emptyList()

            if (symbols.isEmpty()) {
                return@withContext SheetsAnalysisResult(
                    symbols = emptyList(),
                    timestamp = timestamp,
                    success = true,
                    errorMessage = "No suitable symbols found in the sheet"
                )
            }

            // Analyze each symbol
            val analysisResults = mutableListOf<StockAnalysisResult>()

            for (symbol in symbols) {
                try {
                    // Small delay to avoid rate limiting
                    delay(100)

                    val stockAnalysisResult = stockRepository.analyzeStock(symbol)

                    if (stockAnalysisResult.isSuccess) {
                        val stockData = stockAnalysisResult.getOrNull()!!
                        analysisResults.add(
                            StockAnalysisResult(
                                symbol = symbol,
                                currentPrice = stockData.price,
                                signal = stockData.signal.displayName, // Convert SignalType to String
                                boxHigh = stockData.boxHigh,
                                boxLow = stockData.boxLow,
                                volume = stockData.volume,
                                analysisSuccess = true
                            )
                        )
                    } else {
                        analysisResults.add(
                            StockAnalysisResult(
                                symbol = symbol,
                                currentPrice = null,
                                signal = "ERROR",
                                boxHigh = null,
                                boxLow = null,
                                volume = null,
                                analysisSuccess = false,
                                errorMessage = stockAnalysisResult.exceptionOrNull()?.message
                            )
                        )
                    }
                } catch (e: Exception) {
                    analysisResults.add(
                        StockAnalysisResult(
                            symbol = symbol,
                            currentPrice = null,
                            signal = "ERROR",
                            boxHigh = null,
                            boxLow = null,
                            volume = null,
                            analysisSuccess = false,
                            errorMessage = e.message
                        )
                    )
                }
            }

            SheetsAnalysisResult(
                symbols = symbols,
                timestamp = timestamp,
                success = true,
                analysisResults = analysisResults
            )

        } catch (e: Exception) {
            SheetsAnalysisResult(
                symbols = emptyList(),
                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                success = false,
                errorMessage = "Analysis failed: ${e.message}"
            )
        }
    }
}
