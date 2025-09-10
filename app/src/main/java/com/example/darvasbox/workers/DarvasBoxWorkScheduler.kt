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
        Log.d(TAG, "Scheduling market hours analysis (every 10 minutes, 9 AM - 5 PM IST)")

        // Cancel any existing daily schedule
        cancelDailyAnalysis()

        // Schedule the next analysis based on current time
        scheduleNextMarketHoursAnalysis()
    }

    fun scheduleNextMarketHoursAnalysis() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if we can schedule exact alarms (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms. Please enable in device settings.")
                // Fall back to WorkManager periodic scheduling
                scheduleMarketHoursWithWorkManager()
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

        // Calculate next analysis time (every 10 minutes between 9 AM - 5 PM IST)
        val nextScheduleTime = calculateNextMarketAnalysisTime()

        if (nextScheduleTime != null) {
            Log.d(TAG, "Next market analysis scheduled for: ${java.util.Date(nextScheduleTime)}")

            try {
                // Use setExactAndAllowWhileIdle for better reliability
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextScheduleTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        nextScheduleTime,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Market hours analysis alarm set successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule market hours analysis", e)
                scheduleMarketHoursWithWorkManager()
            }
        } else {
            Log.d(TAG, "No market hours today, scheduling for next trading day")
        }
    }

    private fun calculateNextMarketAnalysisTime(): Long? {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))

        // Market hours: 9 AM to 5 PM (17:00) IST
        val marketStart = 9
        val marketEnd = 17

        // Check if today is a weekday (Monday to Friday)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekday = dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY

        if (isWeekday) {
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            // If we're within market hours
            if (currentHour >= marketStart && currentHour < marketEnd) {
                // Calculate next 10-minute slot
                val nextMinute = ((currentMinute / 10) + 1) * 10

                if (nextMinute < 60) {
                    // Next slot is within the same hour
                    calendar.set(Calendar.MINUTE, nextMinute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                } else {
                    // Move to next hour
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    // Check if still within market hours
                    if (calendar.get(Calendar.HOUR_OF_DAY) >= marketEnd) {
                        // Market closed for today, schedule for tomorrow 9 AM
                        return scheduleForNextTradingDay(calendar)
                    }
                }

                return calendar.timeInMillis
            } else if (currentHour < marketStart) {
                // Before market hours, schedule for 9:00 AM today
                calendar.set(Calendar.HOUR_OF_DAY, marketStart)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return calendar.timeInMillis
            }
        }

        // After market hours or weekend, schedule for next trading day
        return scheduleForNextTradingDay(calendar)
    }

    private fun scheduleForNextTradingDay(calendar: Calendar): Long {
        do {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        } while (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

        // Set to 9:00 AM of next trading day
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    private fun scheduleMarketHoursWithWorkManager() {
        Log.d(TAG, "Using WorkManager for market hours scheduling")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false) // Allow analysis even with low battery during market hours
            .build()

        // Schedule analysis every 10 minutes
        val workRequest = PeriodicWorkRequestBuilder<DarvasBoxAnalysisWorker>(10, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("market_hours_analysis")
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "market_hours_darvas_analysis",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Market hours WorkManager scheduled (every 10 minutes)")
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

        // Calculate next market analysis time
        val nextAnalysisTime = calculateNextMarketAnalysisTime()
        val nextTimeString = if (nextAnalysisTime != null) {
            java.util.Date(nextAnalysisTime).toString()
        } else {
            "No trading hours today"
        }

        // Get current market status
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekday = dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY
        val isMarketHours = isWeekday && currentHour >= 9 && currentHour < 17

        val dayNames = arrayOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val currentDayName = dayNames[dayOfWeek]

        return """
            Market Hours Analysis Status:
            - Schedule: Every 10 minutes, 9 AM - 5 PM IST (Weekdays)
            - Can Schedule Exact Alarms: $canScheduleExact
            - Is Scheduled: $isScheduled
            - Current Time: ${java.util.Date()} ($currentDayName)
            - Currently Market Hours: $isMarketHours
            - Next Analysis Time: $nextTimeString
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
