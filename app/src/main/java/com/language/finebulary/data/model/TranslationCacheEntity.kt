package com.language.finebulary.data.model

import androidx.room.Entity

@Entity(tableName = "translation_cache", primaryKeys = ["queryText", "sourceLang", "targetLang"])
data class TranslationCacheEntity(
    val queryText: String,
    val sourceLang: String,
    val targetLang: String,
    val translation: String,
    val definition: String?,
    val synonyms: String?,
    val timestamp: Long = System.currentTimeMillis()
)
