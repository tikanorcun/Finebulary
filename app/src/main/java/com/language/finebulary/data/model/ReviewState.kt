package com.language.finebulary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_states")
data class ReviewState(
    @PrimaryKey
    val wordId: Long, // Links this state to a specific Word id
    val ease: Double = 2.5,
    val intervalDays: Int = 0,
    val dueDateEpochMs: Long = System.currentTimeMillis(),
    val reps: Int = 0,
    val lapses: Int = 0,
    val lastReviewedAtEpochMs: Long? = null
)