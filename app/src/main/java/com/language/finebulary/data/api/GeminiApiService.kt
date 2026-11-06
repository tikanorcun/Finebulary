package com.language.finebulary.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface GeminiApiService {
    @GET("v1beta/models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): GeminiModelListResponse
}
