package com.example.darvasbox.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.darvasbox.MainActivity
import com.example.darvasbox.R
import com.example.darvasbox.data.model.StockData

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "darvas_box_signals"
        const val CHANNEL_NAME = "Darvas Box Trading Signals"
        const val CHANNEL_DESCRIPTION = "Notifications for new BUY/SELL signals from Darvas Box analysis"
        const val ANALYSIS_CHANNEL_ID = "darvas_box_analysis"
        const val ANALYSIS_CHANNEL_NAME = "Darvas Box Analysis Complete"
        const val ANALYSIS_CHANNEL_DESCRIPTION = "Notifications when periodic analysis is completed"

        // Intent extras for deep linking
        const val EXTRA_SHOW_RESULTS = "show_analysis_results"
        const val EXTRA_ANALYSIS_DATA = "analysis_data"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Create signal channel
            val signalChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            // Create analysis completion channel
            val analysisChannel = NotificationChannel(
                ANALYSIS_CHANNEL_ID,
                ANALYSIS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ANALYSIS_CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(signalChannel)
            notificationManager.createNotificationChannel(analysisChannel)
        }
    }

    fun showSignalNotification(stockData: StockData) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("stock_symbol", stockData.symbol)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            stockData.symbol.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${stockData.signal} Signal: ${stockData.symbol}"
        val message = buildNotificationMessage(stockData)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(getSignalIcon(stockData.signal))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                stockData.symbol.hashCode(),
                notification
            )
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            android.util.Log.w("NotificationHelper", "Notification permission not granted", e)
        }
    }

    private fun buildNotificationMessage(stockData: StockData): String {
        val changeText = if ((stockData.change ?: 0.0) >= 0) "+${String.format("%.2f", stockData.change ?: 0.0)}"
                        else String.format("%.2f", stockData.change ?: 0.0)
        val changePercentText = if ((stockData.changePercent ?: 0.0) >= 0) "+${String.format("%.2f", stockData.changePercent ?: 0.0)}%"
                               else "${String.format("%.2f", stockData.changePercent ?: 0.0)}%"

        return when (stockData.signal) {
            "BUY" -> "Price: ₹${String.format("%.2f", stockData.price)} ($changeText, $changePercentText)\n" +
                    "Breakout above box high: ₹${String.format("%.2f", stockData.boxHigh)}\n" +
                    "Volume: ${formatVolume(stockData.volume ?: 0L)}"

            "SELL" -> "Price: ₹${String.format("%.2f", stockData.price)} ($changeText, $changePercentText)\n" +
                     "Breakdown below box low: ₹${String.format("%.2f", stockData.boxLow)}\n" +
                     "Volume: ${formatVolume(stockData.volume ?: 0L)}"

            else -> "Price: ₹${String.format("%.2f", stockData.price)} ($changeText, $changePercentText)"
        }
    }

    private fun getSignalIcon(signal: String): Int {
        return when (signal) {
            "BUY" -> android.R.drawable.ic_menu_add
            "SELL" -> android.R.drawable.ic_menu_delete
            else -> android.R.drawable.ic_dialog_info
        }
    }

    private fun formatVolume(volume: Long): String {
        return when {
            volume >= 10_000_000 -> "${String.format("%.1f", volume / 1_000_000.0)}M"
            volume >= 100_000 -> "${String.format("%.1f", volume / 100_000.0)}L"
            volume >= 1_000 -> "${String.format("%.1f", volume / 1_000.0)}K"
            else -> volume.toString()
        }
    }

    fun showAnalysisCompleteNotification(
        symbolCount: Int,
        buySignals: Int,
        sellSignals: Int,
        analysisData: String // JSON string of analysis results
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_SHOW_RESULTS, true)
            putExtra(EXTRA_ANALYSIS_DATA, analysisData)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            "analysis_complete".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "📊 Analysis Complete"
        val message = "Analyzed $symbolCount stocks • $buySignals BUY • $sellSignals SELL signals found"
        val bigMessage = buildAnalysisNotificationMessage(symbolCount, buySignals, sellSignals)

        val notification = NotificationCompat.Builder(context, ANALYSIS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "View Results",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                "analysis_complete".hashCode(),
                notification
            )
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted", e)
        }
    }

    private fun buildAnalysisNotificationMessage(symbolCount: Int, buySignals: Int, sellSignals: Int): String {
        return """
            📈 Periodic Analysis Results
            
            • Analyzed $symbolCount stocks from Google Sheets
            • Found $buySignals BUY signals
            • Found $sellSignals SELL signals
            
            Tap to view detailed results with price targets and volume data.
        """.trimIndent()
    }
}
