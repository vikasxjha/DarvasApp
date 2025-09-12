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
import com.example.darvasbox.data.model.StockData
import com.example.darvasbox.data.model.SignalType

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "darvas_box_signals"
        const val CHANNEL_NAME = "Darvas Box Signal Changes"
        const val CHANNEL_DESCRIPTION = "Notifications for stock signal changes"
        const val EXTRA_SHOW_RESULTS = "extra_show_results"
        const val EXTRA_ANALYSIS_DATA = "extra_analysis_data"
        private var notificationId = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Check if signal change warrants a notification based on specific transitions:
     * - BUY → SELL
     * - SELL → BUY
     * - IGNORE → BUY
     * - BUY → IGNORE
     */
    fun shouldNotifyForSignalChange(previousSignal: SignalType?, currentSignal: SignalType): Boolean {
        if (previousSignal == null || previousSignal == currentSignal) return false

        return when (previousSignal to currentSignal) {
            SignalType.BUY to SignalType.SELL -> true      // Exit long position
            SignalType.SELL to SignalType.BUY -> true      // Reversal from sell to buy
            SignalType.IGNORE to SignalType.BUY -> true    // New buying opportunity
            SignalType.BUY to SignalType.IGNORE -> true    // Exit buy signal (false breakout)
            else -> false                                  // All other transitions ignored
        }
    }

    /**
     * Overloaded method for backward compatibility with string signals
     */
    fun shouldNotifyForSignalChange(previousSignal: String?, currentSignal: String): Boolean {
        val prevSignalType = previousSignal?.let { SignalType.fromString(it) }
        val currSignalType = SignalType.fromString(currentSignal)
        return shouldNotifyForSignalChange(prevSignalType, currSignalType)
    }

    /**
     * Show notification for significant signal changes
     */
    fun showSignalChangeNotification(stockData: StockData, previousSignal: SignalType) {
        val currentSignal = stockData.signal

        if (!shouldNotifyForSignalChange(previousSignal, currentSignal)) {
            return
        }

        val transitionMessage = getTransitionMessage(previousSignal, currentSignal)
        val actionMessage = getActionMessage(previousSignal, currentSignal)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${stockData.symbol} - $transitionMessage")
            .setContentText("$actionMessage\nPrice: ₹${String.format("%.2f", stockData.price)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$actionMessage\n" +
                        "Price: ₹${String.format("%.2f", stockData.price)}\n" +
                        "Box High: ₹${String.format("%.2f", stockData.boxHigh)}\n" +
                        "Box Low: ₹${String.format("%.2f", stockData.boxLow)}\n" +
                        "Change: ${if (stockData.change >= 0) "+" else ""}${String.format("%.2f", stockData.change)} (${String.format("%.2f", stockData.changePercent)}%)"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(++notificationId, notification)
        } catch (e: SecurityException) {
            // Handle notification permission not granted
            android.util.Log.w("NotificationHelper", "Notification permission not granted", e)
        }
    }

    /**
     * Overloaded method for backward compatibility with string signals
     */
    fun showSignalChangeNotification(stockData: StockData, previousSignal: String) {
        val prevSignalType = SignalType.fromString(previousSignal)
        showSignalChangeNotification(stockData, prevSignalType)
    }

    private fun getTransitionMessage(previousSignal: SignalType, currentSignal: SignalType): String {
        return when (previousSignal to currentSignal) {
            SignalType.BUY to SignalType.SELL -> "BUY → SELL Signal"
            SignalType.SELL to SignalType.BUY -> "SELL → BUY Signal"
            SignalType.IGNORE to SignalType.BUY -> "New BUY Signal"
            SignalType.BUY to SignalType.IGNORE -> "BUY Signal Ended"
            else -> "Signal Changed"
        }
    }

    private fun getActionMessage(previousSignal: SignalType, currentSignal: SignalType): String {
        return when (previousSignal to currentSignal) {
            SignalType.BUY to SignalType.SELL -> "⚠️ Consider selling your position - price broke below support"
            SignalType.SELL to SignalType.BUY -> "🔄 Reversal detected - consider buying opportunity"
            SignalType.IGNORE to SignalType.BUY -> "🚀 New buying opportunity detected with volume confirmation"
            SignalType.BUY to SignalType.IGNORE -> "⏸️ Buy signal weakened - monitor closely or consider exit"
            else -> "Signal status changed"
        }
    }

    /**
     * Show analysis completion notification
     */
    fun showAnalysisCompleteNotification(
        symbolCount: Int,
        buySignals: Int,
        sellSignals: Int,
        analysisData: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_SHOW_RESULTS, true)
            putExtra(EXTRA_ANALYSIS_DATA, analysisData)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Analysis Complete")
            .setContentText("Analyzed $symbolCount stocks: $buySignals BUY, $sellSignals SELL")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Complete analysis finished\n" +
                        "Stocks analyzed: $symbolCount\n" +
                        "BUY signals: $buySignals\n" +
                        "SELL signals: $sellSignals\n" +
                        "Tap to view detailed results"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(++notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted", e)
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    fun showSignalNotification(stockData: StockData) {
        // This method is kept for backward compatibility with existing code
        // For new notifications, use showSignalChangeNotification instead
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${stockData.symbol} - ${stockData.signal.displayName} Signal")
            .setContentText("Price: ₹${String.format("%.2f", stockData.price)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(++notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "Notification permission not granted", e)
        }
    }
}
