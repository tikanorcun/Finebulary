package com.language.finebulary.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleTranslateApiService {
    @GET("translate_a/single")
    suspend fun translate(
        @Query("client") client: String = "gtx",
        @Query("sl") source: String = "auto",
        @Query("tl") target: String,
        @Query("dt") dataType: String = "t",
        @Query("q") text: String
    ): ResponseBody
}
