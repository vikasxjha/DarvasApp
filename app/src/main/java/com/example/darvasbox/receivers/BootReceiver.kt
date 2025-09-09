package com.example.darvasbox.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.darvasbox.DarvasBoxApplication

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                try {
                    Log.d(TAG, "Device boot completed or app replaced - starting Darvas Box cron job")

                    val app = context.applicationContext as DarvasBoxApplication

                    // Set 5-minute interval and start the periodic analysis
                    app.workScheduler.updateAnalysisInterval(5L)
                    app.workScheduler.schedulePeriodicAnalysis()

                    Log.d(TAG, "Successfully started 5-minute Darvas Box analysis cron job after device boot")

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start Darvas Box cron job after boot", e)
                }
            }
        }
    }
}
