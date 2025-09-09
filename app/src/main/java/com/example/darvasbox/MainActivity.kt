package com.example.darvasbox

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.darvasbox.ui.screens.AnalysisResultsScreen
import com.example.darvasbox.ui.screens.DarvasBoxStatusScreen
import com.example.darvasbox.ui.theme.DarvasBoxTheme
import com.example.darvasbox.ui.viewmodel.DarvasBoxViewModel
import com.example.darvasbox.ui.viewmodel.DarvasBoxViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, periodic analysis can show notifications
        } else {
            // Permission denied, analysis will continue but no notifications
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            DarvasBoxTheme {
                val app = applicationContext as DarvasBoxApplication

                // Initialize ViewModel at MainActivity level
                val viewModel: DarvasBoxViewModel = viewModel(
                    factory = DarvasBoxViewModelFactory(
                        app.manualAnalysisService,
                        app.notificationHelper,
                        app.workScheduler
                    )
                )
                val uiState by viewModel.uiState.collectAsState()

                var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }

                // Handle deep linking from notifications
                LaunchedEffect(intent) {
                    handleNotificationIntent(intent) { analysisResult ->
                        currentScreen = Screen.Results(analysisResult)
                    }
                }

                // Handle navigation based on ViewModel state
                LaunchedEffect(uiState.shouldNavigateToResults) {
                    if (uiState.shouldNavigateToResults && uiState.analysisResult != null) {
                        currentScreen = Screen.Results(uiState.analysisResult!!)
                        viewModel.onNavigationCompleted()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val screen = currentScreen) {
                        is Screen.Main -> {
                            DarvasBoxStatusScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { currentScreen = Screen.Settings },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        is Screen.Settings -> {
                            com.example.darvasbox.ui.screens.SettingsScreen(
                                onBackClick = { currentScreen = Screen.Main },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        is Screen.Results -> {
                            AnalysisResultsScreen(
                                analysisResult = screen.result,
                                onBackClick = {
                                    currentScreen = Screen.Main
                                    viewModel.clearResults()
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleNotificationIntent(
        intent: android.content.Intent,
        onNavigateToResults: (com.example.darvasbox.data.model.SheetsAnalysisResult) -> Unit
    ) {
        if (intent.getBooleanExtra(com.example.darvasbox.notification.NotificationHelper.EXTRA_SHOW_RESULTS, false)) {
            val analysisDataJson = intent.getStringExtra(com.example.darvasbox.notification.NotificationHelper.EXTRA_ANALYSIS_DATA)
            if (analysisDataJson != null) {
                try {
                    val gson = com.google.gson.Gson()
                    val analysisResult = gson.fromJson(
                        analysisDataJson,
                        com.example.darvasbox.data.model.SheetsAnalysisResult::class.java
                    )
                    onNavigateToResults(analysisResult)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to parse analysis data", e)
                }
            }
        }
    }
}

sealed class Screen {
    object Main : Screen()
    object Settings : Screen()
    data class Results(val result: com.example.darvasbox.data.model.SheetsAnalysisResult) : Screen()
}
