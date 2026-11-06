package com.language.finebulary.util

import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.ReviewState
import com.language.finebulary.data.model.Word
import com.google.gson.Gson
import com.google.gson.GsonBuilder

data class BackupData(
    val courses: List<Course>? = null,
    val words: List<Word>? = null,
    val reviewStates: List<ReviewState>? = null,
    val settings: Map<String, String>? = null
)

object BackupManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun createBackupJson(
        courses: List<Course>? = null,
        words: List<Word>? = null,
        reviewStates: List<ReviewState>? = null,
        settings: Map<String, String>? = null
    ): String {
        val data = BackupData(courses, words, reviewStates, settings)
        return gson.toJson(data)
    }

    fun parseBackupJson(json: String): BackupData? {
        return try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
