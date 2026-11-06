package com.language.finebulary.data.api

data class GeminiModelListResponse(
    val models: List<GeminiModelInfo>
)

data class GeminiModelInfo(
    val name: String,
    val version: String?,
    val displayName: String?,
    val description: String?,
    val supportedGenerationMethods: List<String>
)
