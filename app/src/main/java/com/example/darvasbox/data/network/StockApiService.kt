package com.example.darvasbox.data.network

import com.example.darvasbox.data.model.DarvasAnalysisResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface StockApiService {

    @GET("analyze")
    suspend fun analyzeStock(
        @Query("symbol") symbol: String
    ): Response<DarvasAnalysisResponse>

    companion object {
        const val BASE_URL = "https://your-backend-api.herokuapp.com/api/v1/"
    }
}
