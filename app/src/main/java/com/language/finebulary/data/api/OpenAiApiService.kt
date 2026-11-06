package com.language.finebulary.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface OpenAiApiService {
    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse

    @GET
    suspend fun listModels(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): OpenAiModelListResponse
}
