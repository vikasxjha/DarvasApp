package com.example.darvasbox.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.example.darvasbox.data.preferences.AnalysisPreferences
import com.example.darvasbox.receivers.DailyAnalysisReceiver
import java.util.*
import java.util.concurrent.TimeUnit

class DarvasBoxWorkScheduler(private val context: Context) {

    companion object {
        const val WORK_NAME = "darvas_box_analysis_work"
        const val WORK_TAG = "darvas_box_periodic"
        const val DAILY_ANALYSIS_REQUEST_CODE = 1001
        private const val TAG = "DarvasBoxWorkScheduler"
    }

    private val preferences = AnalysisPreferences(context)

    fun schedulePeriodicAnalysis() {
        val intervalMinutes = preferences.analysisIntervalMinutes

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DarvasBoxAnalysisWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            (intervalMinutes / 2).coerceAtLeast(15), TimeUnit.MINUTES // Flex period
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .setInitialDelay(1, TimeUnit.MINUTES) // Start after 1 minute
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                5, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE, // Replace to update interval
            workRequest
        )
    }

    fun updateAnalysisInterval(intervalMinutes: Long) {
        preferences.analysisIntervalMinutes = intervalMinutes
        // Reschedule with new interval
        cancelPeriodicAnalysis()
        schedulePeriodicAnalysis()
    }

    fun cancelPeriodicAnalysis() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun isPeriodicAnalysisScheduled(): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME)
            .get()

        return workInfos.any { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        }
    }

    fun getWorkStatus() = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkLiveData(WORK_NAME)

    fun triggerManualAnalysis(): androidx.lifecycle.LiveData<WorkInfo> {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val manualWorkRequest = OneTimeWorkRequestBuilder<DarvasBoxAnalysisWorker>()
            .setConstraints(constraints)
            .addTag("manual_trigger")
            .build()

        WorkManager.getInstance(context).enqueue(manualWorkRequest)

        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(manualWorkRequest.id)
    }

    fun scheduleDailyAnalysis() {
        Log.d(TAG, "Scheduling daily analysis at 10:59 PM IST")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if we can schedule exact alarms (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms. Please enable in device settings.")
                // Fall back to inexact alarm
                scheduleDailyAnalysisInexact()
                return
            }
        }

        val intent = Intent(context, DailyAnalysisReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_ANALYSIS_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate time for 10:59 PM IST
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        calendar.set(Calendar.HOUR_OF_DAY, 22) // 10 PM (22:00 in 24-hour format)
        calendar.set(Calendar.MINUTE, 59)      // 59 minutes
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If the time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        Log.d(TAG, "Daily analysis scheduled for: ${calendar.time}")

        try {
            // Use setExactAndAllowWhileIdle for better reliability
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Daily analysis alarm set successfully for 10:59 PM IST using exact alarm")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact daily analysis, trying inexact", e)
            scheduleDailyAnalysisInexact()
        }
    }

    private fun scheduleDailyAnalysisInexact() {
        Log.d(TAG, "Scheduling daily analysis using inexact alarm")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyAnalysisReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_ANALYSIS_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate time for 10:59 PM IST
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        calendar.set(Calendar.HOUR_OF_DAY, 22)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Daily analysis scheduled using inexact repeating alarm")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule inexact daily analysis", e)
        }
    }

    fun cancelDailyAnalysis() {
        Log.d(TAG, "Cancelling daily analysis")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyAnalysisReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_ANALYSIS_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Daily analysis cancelled")
    }

    fun isDailyAnalysisScheduled(): Boolean {
        return try {
            val intent = Intent(context, DailyAnalysisReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_ANALYSIS_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking daily analysis schedule", e)
            false
        }
    }

    fun getDailyAnalysisStatus(): String {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canScheduleExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val isScheduled = isDailyAnalysisScheduled()

        // Calculate next 10:59 PM IST
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        calendar.set(Calendar.HOUR_OF_DAY, 22)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return """
            Daily Analysis Status:
            - Can Schedule Exact Alarms: $canScheduleExact
            - Is Scheduled: $isScheduled
            - Next Scheduled Time: ${calendar.time}
            - Current Time: ${java.util.Date()}
            - Time Zone: ${TimeZone.getTimeZone("Asia/Kolkata").displayName}
        """.trimIndent()
    }

    // Method to force reschedule daily analysis (useful for debugging)
    fun forceRescheduleDailyAnalysis() {
        Log.d(TAG, "Force rescheduling daily analysis")
        cancelDailyAnalysis()
        scheduleDailyAnalysis()
    }
}
