package com.language.finebulary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val defaultDirection: String = "FRONT_TO_BACK",
    val currentStreak: Int = 0,
    val lastReviewDay: Long = 0,
    val nativeLanguageCode: String = "tr",
    val targetLanguageCode: String = "en",
    val isPersonal: Boolean = false,
    val cantTalkRightNow: Boolean = false
)
