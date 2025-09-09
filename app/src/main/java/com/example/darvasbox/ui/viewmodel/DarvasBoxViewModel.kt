package com.example.darvasbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.darvasbox.data.model.SheetsAnalysisResult
import com.example.darvasbox.data.service.ManualAnalysisService
import com.example.darvasbox.notification.NotificationHelper
import com.example.darvasbox.workers.DarvasBoxWorkScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class DarvasBoxViewModel(
    private val manualAnalysisService: ManualAnalysisService,
    private val notificationHelper: NotificationHelper,
    private val workScheduler: DarvasBoxWorkScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DarvasBoxUiState())
    val uiState: StateFlow<DarvasBoxUiState> = _uiState.asStateFlow()

    // Add StateFlow to track periodic analysis status
    private val _isPeriodicAnalysisRunning = MutableStateFlow(false)
    val isPeriodicAnalysisRunning: StateFlow<Boolean> = _isPeriodicAnalysisRunning.asStateFlow()

    init {
        // Check initial WorkManager status and start if needed
        updatePeriodicAnalysisStatus()
        // Start periodic analysis by default using WorkManager
        if (!isPeriodicAnalysisRunning()) {
            startPeriodicAnalysis()
        }
    }

    private fun updatePeriodicAnalysisStatus() {
        val isRunning = workScheduler.isPeriodicAnalysisScheduled()
        _isPeriodicAnalysisRunning.value = isRunning
        android.util.Log.d("DarvasBoxViewModel", "Periodic analysis status updated: $isRunning")
    }

    fun startPeriodicAnalysis() {
        // Use WorkManager for background persistence
        workScheduler.schedulePeriodicAnalysis()
        updatePeriodicAnalysisStatus()
        android.util.Log.d("DarvasBoxViewModel", "Started periodic analysis via WorkManager")
    }

    fun stopPeriodicAnalysis() {
        // Cancel WorkManager periodic work
        workScheduler.cancelPeriodicAnalysis()
        updatePeriodicAnalysisStatus()
        android.util.Log.d("DarvasBoxViewModel", "Stopped periodic analysis via WorkManager")
    }

    fun refreshPeriodicAnalysisStatus() {
        updatePeriodicAnalysisStatus()
    }

    fun isPeriodicAnalysisRunning(): Boolean {
        return workScheduler.isPeriodicAnalysisScheduled()
    }

    fun triggerCompleteAnalysis(isAutomaticTrigger: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                android.util.Log.d("DarvasBoxViewModel", "Starting complete analysis (automatic: $isAutomaticTrigger)")

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

                // ALWAYS send notification when complete analysis finishes (both manual and automatic)
                if (result.success) {
                    sendAnalysisCompleteNotification(result)
                    android.util.Log.d("DarvasBoxViewModel", "Notification sent for completed analysis")
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
                    shouldNavigateToResults = false
                )

                android.util.Log.e("DarvasBoxViewModel", "Complete analysis failed", e)
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

            android.util.Log.d("DarvasBoxViewModel", "Analysis complete notification sent: $symbolCount stocks, $buySignals BUY, $sellSignals SELL")
        } catch (e: Exception) {
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
    private val notificationHelper: NotificationHelper,
    private val workScheduler: DarvasBoxWorkScheduler
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DarvasBoxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DarvasBoxViewModel(manualAnalysisService, notificationHelper, workScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
