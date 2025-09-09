package com.example.darvasbox.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.example.darvasbox.workers.DarvasBoxAnalysisWorker
import java.util.concurrent.TimeUnit

class DailyAnalysisReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DailyAnalysisReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Daily analysis alarm triggered at 10:59 PM IST - ${java.util.Date()}")

        try {
            // Trigger immediate analysis via WorkManager
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dailyAnalysisWork = OneTimeWorkRequestBuilder<DarvasBoxAnalysisWorker>()
                .setConstraints(constraints)
                .addTag("daily_10_59_pm_${System.currentTimeMillis()}")
                .setInitialDelay(0, TimeUnit.SECONDS) // Execute immediately
                .build()

            WorkManager.getInstance(context).enqueue(dailyAnalysisWork)

            Log.d(TAG, "Daily analysis work enqueued successfully for 10:59 PM IST execution")

            // Schedule next day's alarm immediately after triggering current analysis
            try {
                val app = context.applicationContext as com.example.darvasbox.DarvasBoxApplication
                app.workScheduler.scheduleDailyAnalysis()
                Log.d(TAG, "Next day's alarm scheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule next day's alarm", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger daily analysis", e)

            // Even if analysis fails, try to schedule next day
            try {
                val app = context.applicationContext as com.example.darvasbox.DarvasBoxApplication
                app.workScheduler.scheduleDailyAnalysis()
            } catch (scheduleError: Exception) {
                Log.e(TAG, "Failed to schedule next day after error", scheduleError)
            }
        }
    }
}
