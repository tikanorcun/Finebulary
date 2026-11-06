package com.language.finebulary.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryApiService {
    @GET("api/v2/entries/{lang}/{word}")
    suspend fun getDetails(
        @Path("lang") languageCode: String,
        @Path("word") word: String
    ): List<DictionaryEntry>
}
