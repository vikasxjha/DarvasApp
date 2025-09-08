package com.example.darvasbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.darvasbox.data.model.StockData
import com.example.darvasbox.data.repository.SimpleStockRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StockAnalysisViewModel(
    private val repository: SimpleStockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockAnalysisUiState())
    val uiState: StateFlow<StockAnalysisUiState> = _uiState.asStateFlow()

    val recentStocks = repository.getRecentStocks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stocksWithSignals = repository.getStocksWithSignals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun analyzeStock(symbol: String) {
        if (symbol.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.analyzeStock(symbol.uppercase())
            result.fold(
                onSuccess = { stockData ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentStock = stockData,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error occurred"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // For demo purposes, just clear loading state
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}

class StockAnalysisViewModelFactory(
    private val repository: SimpleStockRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockAnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockAnalysisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class StockAnalysisUiState(
    val isLoading: Boolean = false,
    val currentStock: StockData? = null,
    val error: String? = null
)
