package com.example.darvasbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.darvasbox.data.model.SheetsAnalysisResult
import com.example.darvasbox.data.service.ManualAnalysisService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DarvasBoxViewModel(
    private val manualAnalysisService: ManualAnalysisService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DarvasBoxUiState())
    val uiState: StateFlow<DarvasBoxUiState> = _uiState.asStateFlow()

    fun triggerCompleteAnalysis() {
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
                    shouldNavigateToResults = result.success // Navigate only if successful
                )
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
}

data class DarvasBoxUiState(
    val isLoading: Boolean = false,
    val analysisResult: SheetsAnalysisResult? = null,
    val error: String? = null,
    val shouldNavigateToResults: Boolean = false // Added navigation state
)

class DarvasBoxViewModelFactory(
    private val manualAnalysisService: ManualAnalysisService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DarvasBoxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DarvasBoxViewModel(manualAnalysisService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
