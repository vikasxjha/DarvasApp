package com.example.darvasbox.workers

import android.content.Context
import androidx.work.*
import com.example.darvasbox.data.preferences.AnalysisPreferences
import java.util.concurrent.TimeUnit

class DarvasBoxWorkScheduler(private val context: Context) {

    companion object {
        const val WORK_NAME = "darvas_box_analysis_work"
        const val WORK_TAG = "darvas_box_periodic"
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
}
