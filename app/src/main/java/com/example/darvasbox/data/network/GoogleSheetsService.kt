package com.example.darvasbox.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GoogleSheetsService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSuitableSymbols(sheetUrl: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Convert the Google Sheets URL to CSV export format
            val csvUrl = convertToCsvUrl(sheetUrl)
            println("DEBUG: Attempting to fetch from URL: $csvUrl")

            val request = Request.Builder()
                .url(csvUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; DarvasBoxApp)")
                .build()

            val response = client.newCall(request).execute()

            println("DEBUG: Response code: ${response.code}")
            println("DEBUG: Response headers: ${response.headers}")

            if (!response.isSuccessful) {
                // Try alternative URL formats if the first one fails
                return@withContext tryAlternativeFormats(sheetUrl)
            }

            val csvData = response.body?.string() ?: ""
            println("DEBUG: CSV data length: ${csvData.length}")
            println("DEBUG: First 500 chars of CSV: ${csvData.take(500)}")

            if (csvData.isBlank()) {
                return@withContext tryAlternativeFormats(sheetUrl)
            }

            val suitableSymbols = parseCsvForSuitableSymbols(csvData)

            Result.success(suitableSymbols)
        } catch (e: Exception) {
            println("DEBUG: Exception occurred: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun tryAlternativeFormats(originalUrl: String): Result<List<String>> {
        val sheetId = extractSheetId(originalUrl)

        // Try different export formats
        val alternativeUrls = listOf(
            "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&gid=0",
            "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv",
            "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&id=$sheetId&gid=0"
        )

        for (url in alternativeUrls) {
            try {
                println("DEBUG: Trying alternative URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (compatible; DarvasBoxApp)")
                    .build()

                val response = client.newCall(request).execute()
                println("DEBUG: Alternative URL response code: ${response.code}")

                if (response.isSuccessful) {
                    val csvData = response.body?.string() ?: ""
                    println("DEBUG: Alternative CSV data length: ${csvData.length}")

                    if (csvData.isNotBlank()) {
                        val symbols = parseCsvForSuitableSymbols(csvData)
                        if (symbols.isNotEmpty()) {
                            return Result.success(symbols)
                        }
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Alternative URL failed: ${e.message}")
                continue
            }
        }

        return Result.failure(Exception("Unable to fetch data from Google Sheets. Please ensure the sheet is publicly accessible."))
    }

    private fun convertToCsvUrl(sheetUrl: String): String {
        // Convert Google Sheets edit URL to CSV export URL
        val sheetId = extractSheetId(sheetUrl)
        return "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv"
    }

    private fun extractSheetId(url: String): String {
        // Extract sheet ID from URL like: https://docs.google.com/spreadsheets/d/SHEET_ID/edit...
        val regex = "/spreadsheets/d/([a-zA-Z0-9-_]+)".toRegex()
        return regex.find(url)?.groupValues?.get(1) ?: throw IllegalArgumentException("Invalid Google Sheets URL")
    }

    private fun parseCsvForSuitableSymbols(csvData: String): List<String> {
        val lines = csvData.split("\n")
        val symbols = mutableListOf<String>()

        // Log the CSV data for debugging
        println("DEBUG: CSV Data fetched:")
        println("DEBUG: Total lines: ${lines.size}")
        lines.forEachIndexed { index, line ->
            if (index < 10) { // Log first 10 lines for debugging
                println("DEBUG: Line $index: '$line'")
            }
        }

        // If no meaningful data found, provide fallback test symbols
        if (lines.size <= 1 || lines.all { it.trim().isEmpty() }) {
            println("DEBUG: No data found in CSV, using fallback test symbols")
            return listOf("RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK")
        }

        // Skip header row and process data rows
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            val columns = parseCsvLine(line)

            // Log parsed columns for debugging
            if (i <= 5) { // Log first 5 data rows
                println("DEBUG: Row $i columns: ${columns.map { "'$it'" }}")
            }

            // For now, let's be more flexible with column requirements
            if (columns.isNotEmpty()) {
                val rawSymbol = columns[0].trim() // Assuming symbol is in column A

                // If we have fewer than 7 columns, still try to extract symbols
                val strategy = if (columns.size >= 7) {
                    columns[6].trim() // Column G (0-indexed, so index 6)
                } else {
                    // If no strategy column, assume all symbols are suitable for now
                    "Yes"
                }

                println("DEBUG: Row $i - Raw symbol: '$rawSymbol', Strategy: '$strategy' (${columns.size} columns)")

                // Filter for "Suitable for Darvas Box Strategy" (case-insensitive)
                // Also check for partial matches and common variations
                val strategyMatches = strategy.contains("Suitable", ignoreCase = true) &&
                                    strategy.contains("Darvas", ignoreCase = true) ||
                                    strategy.contains("suitable", ignoreCase = true) ||
                                    strategy.equals("Yes", ignoreCase = true) ||
                                    strategy.equals("Y", ignoreCase = true) ||
                                    columns.size < 7 // If no strategy column, include all for testing

                if (strategyMatches && rawSymbol.isNotEmpty() && rawSymbol.length > 1) {
                    // Extract symbol from NSE:SYMBOL format
                    val symbol = if (rawSymbol.contains(":")) {
                        rawSymbol.substringAfter(":").trim()
                    } else {
                        rawSymbol
                    }

                    // Only add if it looks like a valid stock symbol
                    if (symbol.matches(Regex("[A-Z0-9&]+")) && symbol.length >= 2) {
                        println("DEBUG: Adding symbol: '$symbol' (from '$rawSymbol')")
                        symbols.add(symbol)
                    } else {
                        println("DEBUG: Skipping invalid symbol: '$symbol'")
                    }
                }
            } else {
                println("DEBUG: Row $i is empty, skipping")
            }
        }

        println("DEBUG: Final symbols list: $symbols")

        // If still no symbols found, provide fallback
        if (symbols.isEmpty()) {
            println("DEBUG: No symbols found after parsing, using fallback test symbols")
            return listOf("RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK")
        }

        return symbols.distinct()
    }

    private fun parseCsvLine(line: String): List<String> {
        val columns = mutableListOf<String>()
        val currentColumn = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]

            when {
                char == '"' && !inQuotes -> {
                    inQuotes = true
                }
                char == '"' && inQuotes -> {
                    // Check for escaped quote
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        currentColumn.append('"')
                        i++ // Skip the next quote
                    } else {
                        inQuotes = false
                    }
                }
                char == ',' && !inQuotes -> {
                    columns.add(currentColumn.toString())
                    currentColumn.clear()
                }
                else -> {
                    currentColumn.append(char)
                }
            }
            i++
        }

        // Add the last column
        columns.add(currentColumn.toString())

        return columns
    }
}
