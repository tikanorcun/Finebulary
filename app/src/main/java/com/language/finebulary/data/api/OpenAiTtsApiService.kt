package com.language.finebulary.data.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

data class OpenAiTtsRequest(
    val model: String = "tts-1",
    val input: String,
    val voice: String = "alloy" // alloy, echo, fable, onyx, nova, shimmer
)

interface OpenAiTtsApiService {
    @POST
    suspend fun generateSpeech(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiTtsRequest
    ): ResponseBody
}
