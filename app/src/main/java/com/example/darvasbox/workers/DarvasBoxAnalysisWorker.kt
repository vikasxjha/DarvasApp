package com.example.darvasbox.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.darvasbox.data.network.GoogleSheetsService
import com.example.darvasbox.data.repository.SimpleStockRepository
import com.example.darvasbox.notification.NotificationHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class DarvasBoxAnalysisWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val googleSheetsService: GoogleSheetsService,
    private val stockRepository: SimpleStockRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "DarvasBoxAnalysisWorker"
        const val GOOGLE_SHEET_URL = "https://docs.google.com/spreadsheets/d/1paWhSx9l-sJBYfJBTON0zPgTcuTVqPuUSQGv5NTWgH0/edit?usp=sharing"
        private val previousSignals = mutableMapOf<String, String>() // Track previous signals to detect new ones
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting Darvas Box analysis at ${getCurrentTimeIST()}")

            // Fetch suitable symbols from Google Sheets
            val symbolsResult = googleSheetsService.fetchSuitableSymbols(GOOGLE_SHEET_URL)

            if (symbolsResult.isFailure) {
                Log.e(TAG, "Failed to fetch symbols: ${symbolsResult.exceptionOrNull()?.message}")
                return Result.retry()
            }

            val symbols = symbolsResult.getOrNull() ?: emptyList()
            Log.d(TAG, "Found ${symbols.size} suitable symbols: $symbols")

            if (symbols.isEmpty()) {
                Log.w(TAG, "No suitable symbols found")
                return Result.success()
            }

            // Analyze each symbol
            var newSignalsCount = 0
            val allAnalysisResults = mutableListOf<com.example.darvasbox.data.model.StockAnalysisResult>()

            for (symbol in symbols) {
                try {
                    // Small delay between API calls to avoid rate limiting
                    delay(100)

                    val analysisResult = stockRepository.analyzeStock(symbol)

                    if (analysisResult.isSuccess) {
                        val stockData = analysisResult.getOrNull()!!
                        val currentSignal = stockData.signal
                        val previousSignal = previousSignals[symbol]

                        // Add to complete results list
                        allAnalysisResults.add(
                            com.example.darvasbox.data.model.StockAnalysisResult(
                                symbol = symbol,
                                currentPrice = stockData.price,
                                signal = currentSignal,
                                boxHigh = stockData.boxHigh,
                                boxLow = stockData.boxLow,
                                volume = stockData.volume,
                                analysisSuccess = true
                            )
                        )

                        // Check if this is a new BUY or SELL signal
                        if ((currentSignal == "BUY" || currentSignal == "SELL") &&
                            currentSignal != previousSignal) {

                            Log.d(TAG, "New $currentSignal signal for $symbol")

                            // Show notification for new signal
                            notificationHelper.showSignalNotification(stockData)
                            newSignalsCount++

                            // Update previous signal
                            previousSignals[symbol] = currentSignal
                        } else if (currentSignal == "IGNORE") {
                            // Update to IGNORE if that's the current state
                            previousSignals[symbol] = currentSignal
                        }

                        Log.d(TAG, "Analyzed $symbol: Price=${stockData.price}, Signal=$currentSignal, " +
                                "BoxHigh=${stockData.boxHigh}, BoxLow=${stockData.boxLow}")

                    } else {
                        Log.e(TAG, "Failed to analyze $symbol: ${analysisResult.exceptionOrNull()?.message}")

                        // Add error result
                        allAnalysisResults.add(
                            com.example.darvasbox.data.model.StockAnalysisResult(
                                symbol = symbol,
                                currentPrice = null,
                                signal = "ERROR",
                                boxHigh = null,
                                boxLow = null,
                                volume = null,
                                analysisSuccess = false,
                                errorMessage = analysisResult.exceptionOrNull()?.message
                            )
                        )
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error analyzing $symbol", e)

                    // Add error result
                    allAnalysisResults.add(
                        com.example.darvasbox.data.model.StockAnalysisResult(
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

            // Send completion notification with analysis summary
            val buySignals = allAnalysisResults.count { it.signal == "BUY" }
            val sellSignals = allAnalysisResults.count { it.signal == "SELL" }

            // Create analysis result for notification
            val analysisResultForNotification = com.example.darvasbox.data.model.SheetsAnalysisResult(
                symbols = symbols,
                timestamp = getCurrentTimeIST(),
                success = true,
                analysisResults = allAnalysisResults
            )

            // Convert to JSON for notification
            val gson = com.google.gson.Gson()
            val analysisDataJson = gson.toJson(analysisResultForNotification)

            // Show completion notification
            notificationHelper.showAnalysisCompleteNotification(
                symbolCount = symbols.size,
                buySignals = buySignals,
                sellSignals = sellSignals,
                analysisData = analysisDataJson
            )

            Log.d(TAG, "Analysis completed. Found $newSignalsCount new signals, $buySignals BUY, $sellSignals SELL at ${getCurrentTimeIST()}")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.retry()
        }
    }

    private fun getCurrentTimeIST(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss IST", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return formatter.format(calendar.time)
    }
}
