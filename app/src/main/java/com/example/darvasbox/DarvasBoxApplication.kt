package com.example.darvasbox

import android.app.Application
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.darvasbox.data.database.DarvasBoxDatabase
import com.example.darvasbox.data.network.GoogleSheetsService
import com.example.darvasbox.data.network.StockApiService
import com.example.darvasbox.data.repository.SimpleStockRepository
import com.example.darvasbox.data.service.ManualAnalysisService
import com.example.darvasbox.notification.NotificationHelper
import com.example.darvasbox.workers.DarvasBoxWorkerFactory
import com.example.darvasbox.workers.DarvasBoxWorkScheduler
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DarvasBoxApplication : Application(), Configuration.Provider {

    // Simple dependency container without Hilt
    lateinit var database: DarvasBoxDatabase
        private set

    lateinit var repository: SimpleStockRepository
        private set

    lateinit var googleSheetsService: GoogleSheetsService
        private set

    lateinit var manualAnalysisService: ManualAnalysisService
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    lateinit var workScheduler: DarvasBoxWorkScheduler
        private set

    private lateinit var workerFactory: DarvasBoxWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        database = Room.databaseBuilder(
            applicationContext,
            DarvasBoxDatabase::class.java,
            "darvas_box_database"
        ).build()

        // Initialize API service
        val retrofit = Retrofit.Builder()
            .baseUrl(StockApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(StockApiService::class.java)

        // Initialize repository
        repository = SimpleStockRepository(apiService, database.stockDataDao())

        // Initialize Google Sheets service
        googleSheetsService = GoogleSheetsService()

        // Initialize manual analysis service
        manualAnalysisService = ManualAnalysisService(googleSheetsService, repository)

        // Initialize notification helper
        notificationHelper = NotificationHelper(this)

        // Initialize WorkManager with custom factory
        workerFactory = DarvasBoxWorkerFactory(
            googleSheetsService,
            repository,
            notificationHelper
        )

        val workManagerConfig = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

        WorkManager.initialize(this, workManagerConfig)

        // Initialize and start work scheduler
        workScheduler = DarvasBoxWorkScheduler(this)
        workScheduler.schedulePeriodicAnalysis()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
