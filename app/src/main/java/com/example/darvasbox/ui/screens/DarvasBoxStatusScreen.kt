package com.example.darvasbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import com.example.darvasbox.DarvasBoxApplication
import com.example.darvasbox.data.model.SheetsAnalysisResult
import com.example.darvasbox.data.model.StockAnalysisResult
import com.example.darvasbox.ui.viewmodel.DarvasBoxViewModel
import com.example.darvasbox.ui.viewmodel.DarvasBoxViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarvasBoxStatusScreen(
    viewModel: DarvasBoxViewModel,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as DarvasBoxApplication
    val workStatus by app.workScheduler.getWorkStatus().observeAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isPeriodicAnalysisRunning = viewModel.isPeriodicAnalysisRunning()

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Settings Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Darvas Box Analysis Status",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Monitoring Google Sheets for suitable symbols\nAnalysis runs every 10 minutes (Asia/Kolkata time)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Work Status
        // Automatic Analysis Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isPeriodicAnalysisRunning)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isPeriodicAnalysisRunning)
                                "Running - Analysis every 10 minutes"
                            else
                                "Stopped - Manual analysis only",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPeriodicAnalysisRunning)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Icon(
                        imageVector = if (isPeriodicAnalysisRunning) Icons.Default.PlayArrow else Icons.Default.Stop,
                        contentDescription = if (isPeriodicAnalysisRunning) "Running" else "Stopped",
                        tint = if (isPeriodicAnalysisRunning)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Periodic Work Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                workStatus?.let { workInfoList ->
                    if (workInfoList.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(workInfoList) { workInfo ->
                                WorkInfoItem(workInfo = workInfo)
                            }
                        }
                    } else {
                        Text(
                            text = "No work scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } ?: run {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Google Sheets Info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sheet URL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Google Sheets (Column G filter)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Configured",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Analysis Frequency",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Every 10 minutes",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Scheduled",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Error display section
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Control Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Automatic Analysis Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPeriodicAnalysisRunning) {
                    Button(
                        onClick = { viewModel.stopPeriodicAnalysis() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop Automatic Analysis")
                    }
                } else {
                    Button(
                        onClick = { viewModel.startPeriodicAnalysis() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Automatic Analysis")
                    }
                }
            }

            // Manual Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        app.workScheduler.cancelPeriodicAnalysis()
                        app.workScheduler.schedulePeriodicAnalysis()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restart Analysis")
                }

                Button(
                    onClick = { viewModel.triggerCompleteAnalysis(isAutomaticTrigger = false) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Analyzing...")
                        }
                    } else {
                        Text("Run Complete Analysis")
                    }
                }
            }
        }

        // Automatic Run Button at the bottom
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Start automatic analysis if not running, otherwise trigger a manual run
                if (!isPeriodicAnalysisRunning) {
                    viewModel.startPeriodicAnalysis()
                } else {
                    viewModel.triggerCompleteAnalysis(isAutomaticTrigger = false)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isPeriodicAnalysisRunning) Icons.Default.PlayCircle else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Automatic Run",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AnalysisResultsCard(
    result: SheetsAnalysisResult,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
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
                    text = "Complete Analysis Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close results"
                    )
                }
            }

            Text(
                text = "Executed at: ${result.timestamp}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!result.success) {
                Text(
                    text = "Error: ${result.errorMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Summary
                Text(
                    text = "📊 Found ${result.symbols.size} symbols from Google Sheets",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (result.symbols.isNotEmpty()) {
                    Text(
                        text = "🎯 Symbols: ${result.symbols.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Analysis Summary
                val analysisResults = result.analysisResults
                if (analysisResults.isNotEmpty()) {
                    val buySignals = analysisResults.count { it.signal == "BUY" }
                    val sellSignals = analysisResults.count { it.signal == "SELL" }
                    val ignoreSignals = analysisResults.count { it.signal == "IGNORE" }
                    val errorCount = analysisResults.count { it.signal == "ERROR" }

                    Text(
                        text = "📈 Signals: $buySignals BUY, $sellSignals SELL, $ignoreSignals IGNORE, $errorCount ERRORS",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(analysisResults) { stockResult ->
                            StockAnalysisResultItem(stockResult = stockResult)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockAnalysisResultItem(stockResult: StockAnalysisResult) {
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
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stockResult.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                val signalColor = when (stockResult.signal) {
                    "BUY" -> MaterialTheme.colorScheme.primary
                    "SELL" -> MaterialTheme.colorScheme.error
                    "ERROR" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline
                }

                Text(
                    text = stockResult.signal,
                    style = MaterialTheme.typography.labelMedium,
                    color = signalColor,
                    fontWeight = FontWeight.Bold
                )
            }

            if (stockResult.analysisSuccess) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Price: ₹${stockResult.currentPrice?.let { "%.2f".format(it) } ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        stockResult.volume?.let { volume ->
                            Text(
                                text = "Volume: ${volume}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        stockResult.boxHigh?.let { high ->
                            Text(
                                text = "Box High: ₹${"%.2f".format(high)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        stockResult.boxLow?.let { low ->
                            Text(
                                text = "Box Low: ₹${"%.2f".format(low)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Error: ${stockResult.errorMessage ?: "Analysis failed"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WorkInfoItem(workInfo: WorkInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (workInfo.state) {
                WorkInfo.State.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.tertiaryContainer
                WorkInfo.State.FAILED -> MaterialTheme.colorScheme.errorContainer
                WorkInfo.State.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Work Status: ${workInfo.state.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Tags: ${workInfo.tags.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            val icon = when (workInfo.state) {
                WorkInfo.State.RUNNING -> Icons.Default.PlayArrow
                WorkInfo.State.SUCCEEDED -> Icons.Default.CheckCircle
                WorkInfo.State.FAILED -> Icons.Default.Error
                WorkInfo.State.CANCELLED -> Icons.Default.Cancel
                else -> Icons.Default.Schedule
            }

            val iconColor = when (workInfo.state) {
                WorkInfo.State.RUNNING -> MaterialTheme.colorScheme.primary
                WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.tertiary
                WorkInfo.State.FAILED -> MaterialTheme.colorScheme.error
                WorkInfo.State.CANCELLED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outline
            }

            Icon(
                imageVector = icon,
                contentDescription = workInfo.state.name,
                tint = iconColor
            )
        }
    }
}
