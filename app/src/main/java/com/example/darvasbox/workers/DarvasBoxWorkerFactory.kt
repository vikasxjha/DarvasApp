package com.example.darvasbox.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.darvasbox.data.network.GoogleSheetsService
import com.example.darvasbox.data.repository.SimpleStockRepository
import com.example.darvasbox.notification.NotificationHelper

class DarvasBoxWorkerFactory(
    private val googleSheetsService: GoogleSheetsService,
    private val stockRepository: SimpleStockRepository,
    private val notificationHelper: NotificationHelper
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            DarvasBoxAnalysisWorker::class.java.name -> {
                DarvasBoxAnalysisWorker(
                    appContext,
                    workerParameters,
                    googleSheetsService,
                    stockRepository,
                    notificationHelper
                )
            }
            else -> null
        }
    }
}
