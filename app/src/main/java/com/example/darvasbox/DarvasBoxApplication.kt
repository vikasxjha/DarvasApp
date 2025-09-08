package com.example.darvasbox

import android.app.Application
import androidx.room.Room
import com.example.darvasbox.data.database.DarvasBoxDatabase
import com.example.darvasbox.data.network.StockApiService
import com.example.darvasbox.data.repository.SimpleStockRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DarvasBoxApplication : Application() {

    // Simple dependency container without Hilt
    lateinit var database: DarvasBoxDatabase
        private set

    lateinit var repository: SimpleStockRepository
        private set

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
    }
}
