package com.example.darvasbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.darvasbox.ui.screens.StockAnalysisScreen
import com.example.darvasbox.ui.theme.DarvasBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DarvasBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StockAnalysisScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
