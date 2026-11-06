package com.language.finebulary.util

import android.util.Log
import com.language.finebulary.data.api.PexelsApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageSearchManager @Inject constructor(
    private val pexelsApi: PexelsApiService
) {
    suspend fun searchImage(query: String, apiKey: String): String? {
        if (apiKey.isBlank()) return null
        
        return try {
            val response = pexelsApi.searchPhotos(apiKey, query, 1)
            response.photos.firstOrNull()?.src?.medium
        } catch (e: Exception) {
            Log.e("ImageSearchManager", "Pexels API Error: ${e.message}")
            null
        }
    }
}
