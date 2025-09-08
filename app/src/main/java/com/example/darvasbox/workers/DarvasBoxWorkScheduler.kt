package com.example.darvasbox.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class DarvasBoxWorkScheduler(private val context: Context) {

    companion object {
        const val WORK_NAME = "darvas_box_analysis_work"
        const val WORK_TAG = "darvas_box_periodic"
    }

    fun schedulePeriodicAnalysis() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DarvasBoxAnalysisWorker>(
            10, TimeUnit.MINUTES, // Repeat every 10 minutes
            5, TimeUnit.MINUTES   // Flex period of 5 minutes
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
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun cancelPeriodicAnalysis() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
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
