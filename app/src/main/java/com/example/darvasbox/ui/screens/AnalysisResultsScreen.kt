package com.example.darvasbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.darvasbox.data.model.SheetsAnalysisResult
import com.example.darvasbox.data.model.StockAnalysisResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultsScreen(
    analysisResult: SheetsAnalysisResult,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "📊 Analysis Results",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card with Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (analysisResult.success)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = if (analysisResult.success) "✅ Analysis Completed Successfully" else "❌ Analysis Failed",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Executed at: ${analysisResult.timestamp}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        if (analysisResult.success) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "📈 Found ${analysisResult.symbols.size} symbols from Google Sheets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )

                            if (analysisResult.symbols.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🎯 Symbols: ${analysisResult.symbols.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Signal Summary
                            val analysisResults = analysisResult.analysisResults
                            if (analysisResults.isNotEmpty()) {
                                val buySignals = analysisResults.count { it.signal == "BUY" }
                                val sellSignals = analysisResults.count { it.signal == "SELL" }
                                val ignoreSignals = analysisResults.count { it.signal == "IGNORE" }
                                val errorCount = analysisResults.count { it.signal == "ERROR" }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    SignalSummaryItem("BUY", buySignals, MaterialTheme.colorScheme.primary)
                                    SignalSummaryItem("SELL", sellSignals, MaterialTheme.colorScheme.error)
                                    SignalSummaryItem("IGNORE", ignoreSignals, MaterialTheme.colorScheme.outline)
                                    SignalSummaryItem("ERROR", errorCount, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Error: ${analysisResult.errorMessage}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Trading Signals Section
            if (analysisResult.success && analysisResult.analysisResults.isNotEmpty()) {
                // BUY Signals
                val buySignals = analysisResult.analysisResults.filter { it.signal == "BUY" }
                if (buySignals.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "🟢 BUY Signals (${buySignals.size})",
                            subtitle = "Stocks breaking above Darvas Box"
                        )
                    }

                    items(buySignals) { stockResult ->
                        StockResultCard(stockResult = stockResult)
                    }
                }

                // SELL Signals
                val sellSignals = analysisResult.analysisResults.filter { it.signal == "SELL" }
                if (sellSignals.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "🔴 SELL Signals (${sellSignals.size})",
                            subtitle = "Stocks breaking below Darvas Box"
                        )
                    }

                    items(sellSignals) { stockResult ->
                        StockResultCard(stockResult = stockResult)
                    }
                }

                // Other Signals (IGNORE)
                val otherSignals = analysisResult.analysisResults.filter { it.signal == "IGNORE" }
                if (otherSignals.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "⚪ No Action (${otherSignals.size})",
                            subtitle = "Stocks within Darvas Box range"
                        )
                    }

                    items(otherSignals) { stockResult ->
                        StockResultCard(stockResult = stockResult)
                    }
                }

                // Error Signals
                val errorSignals = analysisResult.analysisResults.filter { it.signal == "ERROR" }
                if (errorSignals.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "⚠️ Analysis Errors (${errorSignals.size})",
                            subtitle = "Stocks that failed analysis"
                        )
                    }

                    items(errorSignals) { stockResult ->
                        StockResultCard(stockResult = stockResult)
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SignalSummaryItem(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun StockResultCard(stockResult: StockAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (stockResult.signal) {
                "BUY" -> MaterialTheme.colorScheme.primaryContainer
                "SELL" -> MaterialTheme.colorScheme.errorContainer
                "ERROR" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stockResult.symbol,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                val signalColor = when (stockResult.signal) {
                    "BUY" -> MaterialTheme.colorScheme.primary
                    "SELL" -> MaterialTheme.colorScheme.error
                    "ERROR" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }

                Surface(
                    color = signalColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stockResult.signal,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (stockResult.analysisSuccess) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Price",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₹${stockResult.currentPrice?.let { "%.2f".format(it) } ?: "N/A"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        stockResult.volume?.let { volume ->
                            Text(
                                text = "Volume",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = volume.toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        stockResult.boxHigh?.let { high ->
                            Text(
                                text = "Box High",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "₹${"%.2f".format(high)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        stockResult.boxLow?.let { low ->
                            Text(
                                text = "Box Low",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "₹${"%.2f".format(low)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "❌ Analysis Error: ${stockResult.errorMessage ?: "Unknown error occurred"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
