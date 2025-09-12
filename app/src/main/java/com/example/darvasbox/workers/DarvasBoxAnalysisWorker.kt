package com.example.darvasbox.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.darvasbox.data.network.GoogleSheetsService
import com.example.darvasbox.data.repository.SimpleStockRepository
import com.example.darvasbox.notification.NotificationHelper
import com.example.darvasbox.data.model.SignalType
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
        private val previousSignals = mutableMapOf<String, SignalType>() // Track previous signals with type safety
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting Darvas Box analysis at ${getCurrentTimeIST()}")

            // Check if we're in market hours (9 AM - 5 PM IST, weekdays only)
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val isWeekday = dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY
            val isMarketHours = isWeekday && currentHour >= 9 && currentHour < 17

            // If not in market hours, skip analysis but return success
            if (!isMarketHours) {
                val dayNames = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                Log.d(TAG, "Analysis skipped - outside market hours. Current: ${dayNames[dayOfWeek]} ${currentHour}:${calendar.get(Calendar.MINUTE)}")
                return Result.success()
            }

            Log.d(TAG, "Within market hours - proceeding with analysis")

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
                                signal = currentSignal.displayName, // Convert SignalType to string for compatibility
                                boxHigh = stockData.boxHigh,
                                boxLow = stockData.boxLow,
                                volume = stockData.volume,
                                analysisSuccess = true
                            )
                        )

                        // Check for specific signal transitions that warrant notifications
                        // Only notify for: BUY→SELL, SELL→BUY, IGNORE→BUY, BUY→IGNORE
                        if (notificationHelper.shouldNotifyForSignalChange(previousSignal, currentSignal)) {
                            Log.d(TAG, "Signal transition detected for $symbol: $previousSignal → $currentSignal")

                            // Show notification for this specific transition
                            notificationHelper.showSignalChangeNotification(stockData, previousSignal ?: SignalType.UNKNOWN)
                            newSignalsCount++
                        }

                        // Always update the previous signal to current state
                        previousSignals[symbol] = currentSignal

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
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return "${formatter.format(calendar.time)} IST"
    }
}
