package com.language.finebulary.util

import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.Word

interface AiProvider {
    suspend fun fetchModels(apiKey: String): List<String>
    
    suspend fun generateCourse(
        apiKey: String,
        modelId: String,
        topic: String,
        targetLang: String,
        nativeLang: String,
        wordCount: Int = 15,
        level: String = "B2"
    ): Pair<Course, List<Word>>

    suspend fun generateIeltsReading(
        apiKey: String,
        modelId: String,
        targetLang: String,
        nativeLang: String,
        level: String = "B2",
        topicPrompt: String? = null
    ): com.language.finebulary.data.api.IeltsPassage

    suspend fun generateWordDetails(
        apiKey: String,
        modelId: String,
        word: String,
        targetLang: String,
        nativeLang: String
    ): RichWordDetails
}
