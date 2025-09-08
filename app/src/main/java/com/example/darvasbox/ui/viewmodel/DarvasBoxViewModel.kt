package com.example.darvasbox.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.darvasbox.data.model.SheetsAnalysisResult
import com.example.darvasbox.data.service.ManualAnalysisService
import com.example.darvasbox.notification.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class DarvasBoxViewModel(
    private val manualAnalysisService: ManualAnalysisService,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(DarvasBoxUiState())
    val uiState: StateFlow<DarvasBoxUiState> = _uiState.asStateFlow()

    // Periodic analysis job
    private var periodicAnalysisJob: Job? = null
    private var isPeriodicAnalysisEnabled = false

    init {
        // Start periodic analysis by default after a short delay
        // This ensures all dependencies are properly initialized
        startPeriodicAnalysis()
    }

    fun startPeriodicAnalysis() {
        if (isPeriodicAnalysisEnabled) return // Already running

        isPeriodicAnalysisEnabled = true
        periodicAnalysisJob?.cancel()

        periodicAnalysisJob = viewModelScope.launch {
            // Add initial delay to ensure proper initialization
            delay(5000L) // 5 seconds initial delay

            while (isPeriodicAnalysisEnabled) {
                // Only trigger if not already loading to avoid overlapping analyses
                if (!_uiState.value.isLoading) {
                    try {
                        android.util.Log.d("DarvasBoxViewModel", "Starting automatic analysis...")
                        triggerCompleteAnalysis(isAutomaticTrigger = true)
                    } catch (e: Exception) {
                        android.util.Log.e("DarvasBoxViewModel", "Error in automatic analysis", e)
                    }
                }

                // Wait 10 minutes before next analysis
                delay(10 * 60 * 1000L) // 10 minutes in milliseconds
            }
        }
    }

    fun stopPeriodicAnalysis() {
        isPeriodicAnalysisEnabled = false
        periodicAnalysisJob?.cancel()
        periodicAnalysisJob = null
    }

    fun isPeriodicAnalysisRunning(): Boolean = isPeriodicAnalysisEnabled

    fun triggerCompleteAnalysis(isAutomaticTrigger: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // This single call does everything:
                // 1. Fetches symbols from Google Sheets
                // 2. Analyzes each symbol for Darvas Box signals
                // 3. Returns complete results
                val result = manualAnalysisService.performManualAnalysis()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysisResult = result,
                    error = if (!result.success) result.errorMessage else null,
                    shouldNavigateToResults = result.success && !isAutomaticTrigger // Navigate only if successful and manual
                )

                // Send notification when analysis completes (especially for automatic triggers)
                if (result.success && isAutomaticTrigger) {
                    sendAnalysisCompleteNotification(result)
                }
            } catch (e: Exception) {
                val errorResult = SheetsAnalysisResult(
                    symbols = emptyList(),
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    success = false,
                    errorMessage = "Failed to perform analysis: ${e.message}"
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    analysisResult = errorResult,
                    error = e.message,
                    shouldNavigateToResults = false // Ensure navigation state is false on error
                )
            }
        }
    }

    private fun sendAnalysisCompleteNotification(result: SheetsAnalysisResult) {
        try {
            val buySignals = result.analysisResults.count { it.signal == "BUY" }
            val sellSignals = result.analysisResults.count { it.signal == "SELL" }
            val symbolCount = result.symbols.size

            // Convert analysis results to JSON for passing to notification intent
            val analysisDataJson = try {
                Gson().toJson(result)
            } catch (e: Exception) {
                "{\"error\": \"Failed to serialize analysis data\"}"
            }

            notificationHelper.showAnalysisCompleteNotification(
                symbolCount = symbolCount,
                buySignals = buySignals,
                sellSignals = sellSignals,
                analysisData = analysisDataJson
            )
        } catch (e: Exception) {
            // Log error but don't crash the app
            android.util.Log.e("DarvasBoxViewModel", "Failed to send notification", e)
        }
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            analysisResult = null,
            error = null,
            shouldNavigateToResults = false // Reset navigation state
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onNavigationCompleted() {
        _uiState.value = _uiState.value.copy(shouldNavigateToResults = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopPeriodicAnalysis()
    }
}

data class DarvasBoxUiState(
    val isLoading: Boolean = false,
    val analysisResult: SheetsAnalysisResult? = null,
    val error: String? = null,
    val shouldNavigateToResults: Boolean = false // Added navigation state
)

class DarvasBoxViewModelFactory(
    private val manualAnalysisService: ManualAnalysisService,
    private val notificationHelper: NotificationHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DarvasBoxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DarvasBoxViewModel(manualAnalysisService, notificationHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
