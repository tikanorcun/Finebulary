package com.language.finebulary.data.api

import com.google.gson.annotations.SerializedName

data class MyMemoryResponse(
    @SerializedName("responseData")
    val responseData: TranslationData,
    @SerializedName("responseStatus")
    val responseStatus: Int
)

data class TranslationData(
    @SerializedName("translatedText")
    val translatedText: String
)
