package com.example.darvasbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.darvasbox.DarvasBoxApplication
import com.example.darvasbox.data.model.StockData
import com.example.darvasbox.data.model.SignalType
import com.example.darvasbox.ui.viewmodel.StockAnalysisViewModel
import com.example.darvasbox.ui.viewmodel.StockAnalysisViewModelFactory
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAnalysisScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as DarvasBoxApplication

    val viewModel: StockAnalysisViewModel = viewModel(
        factory = StockAnalysisViewModelFactory(application.repository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentStocks by viewModel.recentStocks.collectAsStateWithLifecycle()
    val stocksWithSignals by viewModel.stocksWithSignals.collectAsStateWithLifecycle()

    var searchText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            // Error will be shown in UI, auto-clear after some time
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Darvas Box Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it.uppercase() },
                    label = { Text("Enter Stock Symbol") },
                    placeholder = { Text("e.g., RELIANCE.NS, TCS.NS") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.analyzeStock(searchText)
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.analyzeStock(searchText)
                        keyboardController?.hide()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && searchText.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Analyze Stock")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error Display
        uiState.error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Current Stock Analysis
        uiState.currentStock?.let { currentStock ->
            StockAnalysisCard(stock = currentStock)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recent Stocks and Signals
        LazyColumn {
            if (stocksWithSignals.isNotEmpty()) {
                item {
                    Text(
                        text = "Stocks with Signals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(stocksWithSignals) { stock ->
                    StockItem(stock = stock)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (recentStocks.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(recentStocks) { stock ->
                    StockItem(stock = stock)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StockAnalysisCard(stock: StockData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                    text = stock.symbol,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                SignalChip(signal = stock.signal)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Price Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Price",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%.2f", stock.price)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Show change percentage
                    if (stock.changePercent != null) {
                        val changeColor = if (stock.changePercent >= 0) Color.Green else Color.Red
                        val changeSymbol = if (stock.changePercent >= 0) "+" else ""
                        Text(
                            text = "$changeSymbol${String.format(Locale.getDefault(), "%.2f", stock.changePercent)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = changeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Darvas Box",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Box High
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "High: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%.2f", stock.boxHigh)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Green,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Box Low
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Low: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%.2f", stock.boxLow)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Box Range
                    val boxRange = stock.boxHigh - stock.boxLow
                    val boxRangePercent = (boxRange / stock.boxLow) * 100
                    Text(
                        text = "Range: ${String.format(Locale.getDefault(), "%.1f", boxRangePercent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Signal Explanation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (stock.signal) {
                        SignalType.BUY -> Color.Green.copy(alpha = 0.1f)
                        SignalType.SELL -> Color.Red.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Signal Explanation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val explanation = when (stock.signal) {
                        SignalType.BUY -> "Price broke above box high (₹${String.format(Locale.getDefault(), "%.2f", stock.boxHigh)}) with strong volume"
                        SignalType.SELL -> "Price broke below box low (₹${String.format(Locale.getDefault(), "%.2f", stock.boxLow)})"
                        else -> "Price is within the Darvas Box range. Wait for breakout or breakdown."
                    }

                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Volume Information
            if (stock.volume != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Volume: ${formatVolume(stock.volume)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Show volume status for breakouts
                    if (stock.signal == SignalType.BUY) {
                        Text(
                            text = "High Volume ✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockItem(stock: StockData) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = stock.symbol,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.2f", stock.price)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            SignalChip(signal = stock.signal)
        }
    }
}

@Composable
fun SignalChip(signal: SignalType) {
    val (backgroundColor, textColor) = when (signal) {
        SignalType.BUY -> Color.Green to Color.White
        SignalType.SELL -> Color.Red to Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.background(
            backgroundColor,
            RoundedCornerShape(16.dp)
        ),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = signal.displayName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatVolume(volume: Long): String {
    return when {
        volume >= 10_000_000 -> "${String.format(Locale.getDefault(), "%.1f", volume / 1_000_000.0)}M"
        volume >= 100_000 -> "${String.format(Locale.getDefault(), "%.1f", volume / 100_000.0)}L"
        volume >= 1_000 -> "${String.format(Locale.getDefault(), "%.1f", volume / 1_000.0)}K"
        else -> volume.toString()
    }
}
