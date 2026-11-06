package com.language.finebulary.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class PexelsResponse(
    val photos: List<PexelsPhoto>
)

data class PexelsPhoto(
    val src: PexelsSrc
)

data class PexelsSrc(
    val medium: String
)

interface PexelsApiService {
    @GET("v1/search")
    suspend fun searchPhotos(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1
    ): PexelsResponse
}
