package com.language.finebulary.data.api

import retrofit2.http.GET
import retrofit2.http.Query

data class DatamuseResult(
    val word: String,
    val score: Int,
    val defs: List<String>? = null
)

interface DatamuseApiService {
    @GET("words")
    suspend fun getSuggestions(
        @Query("sp") spelledLike: String,
        @Query("max") max: Int = 3
    ): List<DatamuseResult>

    @GET("words")
    suspend fun getDetails(
        @Query("sp") spelledLike: String,
        @Query("md") metadata: String = "d",
        @Query("max") max: Int = 1
    ): List<DatamuseResult>
    
    @GET("words")
    suspend fun getSynonyms(
        @Query("rel_syn") word: String,
        @Query("max") max: Int = 5
    ): List<DatamuseResult>

    @GET("words")
    suspend fun getAntonyms(
        @Query("rel_ant") word: String,
        @Query("max") max: Int = 5
    ): List<DatamuseResult>
}
