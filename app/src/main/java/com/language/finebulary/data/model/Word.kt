package com.language.finebulary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val front: String,
    val backsPacked: String, // Main translation(s)
    val definition: String? = null,
    val synonyms: String? = null,
    val antonyms: String? = null,
    val example: String? = null,
    val imageUrl: String? = null
)
