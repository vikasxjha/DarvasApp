package com.example.darvasbox.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.darvasbox.data.dao.StockDataDao
import com.example.darvasbox.data.model.StockData

@Database(
    entities = [StockData::class],
    version = 1,
    exportSchema = false
)
abstract class DarvasBoxDatabase : RoomDatabase() {

    abstract fun stockDataDao(): StockDataDao

    companion object {
        @Volatile
        private var INSTANCE: DarvasBoxDatabase? = null

        fun getDatabase(context: Context): DarvasBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DarvasBoxDatabase::class.java,
                    "darvas_box_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
