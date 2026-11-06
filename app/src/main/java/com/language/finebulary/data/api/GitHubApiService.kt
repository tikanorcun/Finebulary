package com.language.finebulary.data.api

import retrofit2.http.GET

interface GitHubApiService {
    @GET("repos/tikanorcun/Finebulary/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}
