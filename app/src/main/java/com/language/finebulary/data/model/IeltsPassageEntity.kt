package com.language.finebulary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.language.finebulary.data.api.AiGeneratedWord
import com.language.finebulary.data.api.IeltsQuestion

@Entity(tableName = "ielts_passages")
data class IeltsPassageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val passage: String,
    val translatedPassage: String?,
    val questionsJson: String,
    val keywordsJson: String,
    val userAnswersJson: String? = null,
    val level: String,
    val nativeLangCode: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

class IeltsConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromQuestionList(value: List<IeltsQuestion>): String = gson.toJson(value)

    @TypeConverter
    fun toQuestionList(value: String): List<IeltsQuestion> {
        val listType = object : TypeToken<List<IeltsQuestion>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromWordList(value: List<AiGeneratedWord>): String = gson.toJson(value)

    @TypeConverter
    fun toWordList(value: String): List<AiGeneratedWord> {
        val listType = object : TypeToken<List<AiGeneratedWord>>() {}.type
        return gson.fromJson(value, listType)
    }
}
