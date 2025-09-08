package com.example.darvasbox.data.dao

import androidx.room.*
import com.example.darvasbox.data.model.StockData
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDataDao {

    @Query("SELECT * FROM stock_data ORDER BY timestamp DESC LIMIT 10")
    fun getRecentStocks(): Flow<List<StockData>>

    @Query("SELECT * FROM stock_data WHERE symbol = :symbol LIMIT 1")
    suspend fun getStockBySymbol(symbol: String): StockData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stockData: StockData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockData>)

    @Delete
    suspend fun deleteStock(stockData: StockData)

    @Query("DELETE FROM stock_data WHERE timestamp < :cutoffTime")
    suspend fun deleteOldRecords(cutoffTime: Long)

    @Query("SELECT * FROM stock_data WHERE signal IN ('BUY', 'SELL') ORDER BY timestamp DESC")
    fun getStocksWithSignals(): Flow<List<StockData>>
}
