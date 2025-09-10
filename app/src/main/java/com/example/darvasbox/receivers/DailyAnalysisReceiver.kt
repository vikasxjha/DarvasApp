package com.example.darvasbox.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.example.darvasbox.workers.DarvasBoxAnalysisWorker
import java.util.concurrent.TimeUnit
import java.util.*

class DailyAnalysisReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyAnalysisReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Market hours analysis alarm triggered - ${java.util.Date()}")

        try {
            // Check if we're currently within market hours (9 AM - 5 PM IST, weekdays only)
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val isWeekday = dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY
            val isMarketHours = isWeekday && currentHour >= 9 && currentHour < 17

            if (isMarketHours) {
                Log.d(TAG, "Within market hours, executing analysis")

                // Trigger immediate analysis via WorkManager
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val marketAnalysisWork = OneTimeWorkRequestBuilder<DarvasBoxAnalysisWorker>()
                    .setConstraints(constraints)
                    .addTag("market_hours_${System.currentTimeMillis()}")
                    .setInitialDelay(0, TimeUnit.SECONDS) // Execute immediately
                    .build()

                WorkManager.getInstance(context).enqueue(marketAnalysisWork)

                Log.d(TAG, "Market hours analysis work enqueued successfully")
            } else {
                Log.d(TAG, "Outside market hours, skipping analysis")
            }

            // Always reschedule next analysis regardless of whether we executed or not
            try {
                val app = context.applicationContext as com.example.darvasbox.DarvasBoxApplication
                app.workScheduler.scheduleNextMarketHoursAnalysis()
                Log.d(TAG, "Next market hours analysis scheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule next market hours analysis", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process market hours analysis", e)

            // Even if analysis fails, try to schedule next analysis
            try {
                val app = context.applicationContext as com.example.darvasbox.DarvasBoxApplication
                app.workScheduler.scheduleNextMarketHoursAnalysis()
            } catch (scheduleError: Exception) {
                Log.e(TAG, "Failed to schedule next analysis after error", scheduleError)
            }
        }
    }
}
