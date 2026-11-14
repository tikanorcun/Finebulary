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
        return searchImages(query, apiKey, 1).firstOrNull()
    }

    suspend fun searchImages(query: String, apiKey: String, count: Int = 4): List<String> {
        if (apiKey.isBlank()) return emptyList()
        
        return try {
            val response = pexelsApi.searchPhotos(apiKey, query, count)
            response.photos.map { it.src.medium }
        } catch (e: Exception) {
            Log.e("ImageSearchManager", "Pexels API Error: ${e.message}")
            emptyList()
        }
    }
}
