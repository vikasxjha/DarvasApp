package com.example.darvasbox.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class YFinanceData(
    val symbol: String,
    val currentPrice: Double,
    val previousClose: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val volume: Long,
    val marketCap: Long?,
    val currency: String,
    val historicalData: List<HistoricalPrice> = emptyList()
)

data class HistoricalPrice(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

class YFinanceService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getStockData(symbol: String): Result<YFinanceData> = withContext(Dispatchers.IO) {
        try {
            // Convert NSE symbols to Yahoo Finance format if needed
            val yfinanceSymbol = convertToYFinanceSymbol(symbol)

            // Get current price and basic info
            val currentData = getCurrentStockData(yfinanceSymbol)

            // Get historical data for Darvas Box analysis (last 30 days)
            val historicalData = getHistoricalData(yfinanceSymbol, "30d")

            val stockData = YFinanceData(
                symbol = symbol,
                currentPrice = currentData.first,
                previousClose = currentData.second,
                dayHigh = currentData.third,
                dayLow = currentData.fourth,
                volume = currentData.fifth,
                marketCap = currentData.sixth,
                currency = "INR",
                historicalData = historicalData
            )

            Result.success(stockData)
        } catch (e: Exception) {
            println("DEBUG: YFinance error for $symbol: ${e.message}")
            Result.failure(e)
        }
    }

    private fun getCurrentStockData(symbol: String): Tuple6<Double, Double, Double, Double, Long, Long?> {
        // Using Yahoo Finance v8 API endpoint
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (compatible; DarvasBoxApp)")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Failed to fetch data for $symbol: HTTP ${response.code}")
        }

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val json = JSONObject(responseBody)

        val chart = json.getJSONObject("chart")
        val result = chart.getJSONArray("result").getJSONObject(0)
        val meta = result.getJSONObject("meta")

        val currentPrice = meta.optDouble("regularMarketPrice", 0.0)
        val previousClose = meta.optDouble("previousClose", 0.0)
        val dayHigh = meta.optDouble("regularMarketDayHigh", 0.0)
        val dayLow = meta.optDouble("regularMarketDayLow", 0.0)
        val volume = meta.optLong("regularMarketVolume", 0L)
        val marketCap = meta.optLong("marketCap", 0L)

        if (currentPrice == 0.0) {
            throw Exception("Invalid price data for $symbol")
        }

        return Tuple6(currentPrice, previousClose, dayHigh, dayLow, volume, marketCap)
    }

    private fun getHistoricalData(symbol: String, period: String): List<HistoricalPrice> {
        try {
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol?range=$period&interval=1d"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; DarvasBoxApp)")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return emptyList()
            }

            val responseBody = response.body?.string() ?: return emptyList()
            val json = JSONObject(responseBody)

            val chart = json.getJSONObject("chart")
            val result = chart.getJSONArray("result").getJSONObject(0)

            val timestamps = result.getJSONArray("timestamp")
            val indicators = result.getJSONObject("indicators")
            val quote = indicators.getJSONArray("quote").getJSONObject(0)

            val opens = quote.optJSONArray("open")
            val highs = quote.optJSONArray("high")
            val lows = quote.optJSONArray("low")
            val closes = quote.optJSONArray("close")
            val volumes = quote.optJSONArray("volume")

            val historicalPrices = mutableListOf<HistoricalPrice>()

            for (i in 0 until timestamps.length()) {
                val timestamp = timestamps.getLong(i)
                val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestamp * 1000))

                val open = opens?.optDouble(i, 0.0) ?: 0.0
                val high = highs?.optDouble(i, 0.0) ?: 0.0
                val low = lows?.optDouble(i, 0.0) ?: 0.0
                val close = closes?.optDouble(i, 0.0) ?: 0.0
                val volume = volumes?.optLong(i, 0L) ?: 0L

                if (close > 0.0) {
                    historicalPrices.add(
                        HistoricalPrice(date, open, high, low, close, volume)
                    )
                }
            }

            return historicalPrices
        } catch (e: Exception) {
            println("DEBUG: Historical data error for $symbol: ${e.message}")
            return emptyList()
        }
    }

    private fun convertToYFinanceSymbol(symbol: String): String {
        return when {
            symbol.contains(".NS") -> symbol // Already in Yahoo Finance format
            symbol.contains(":") -> {
                // Convert NSE:RELIANCE to RELIANCE.NS
                val stockSymbol = symbol.substringAfter(":").trim()
                "$stockSymbol.NS"
            }
            else -> "$symbol.NS" // Add .NS suffix for NSE stocks
        }
    }
}

// Helper data class for tuple return
data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F?
)
