package com.language.finebulary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_progress")
data class DailyProgress(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val wordsLearned: Int = 0
)
