package com.language.finebulary.data.api

data class AiGeneratedCourse(
    val name: String,
    val words: List<AiGeneratedWord>
)

data class IeltsPassage(
    val title: String,
    val passage: String,
    val translatedPassage: String? = null,
    val questions: List<IeltsQuestion>,
    val academicKeywords: List<AiGeneratedWord>
)

data class IeltsQuestion(
    val type: String, // "MULTIPLE_CHOICE", "TFNG", "GAP_FILL"
    val question: String,
    val options: List<String>? = null,
    val correctIndex: Int? = null,
    val correctAnswer: String? = null,
    val explanation: String
)

data class AiGeneratedWord(
    val front: String,
    val back: String,
    val definition: String? = null,
    val synonyms: String? = null,
    val antonyms: String? = null,
    val example: String? = null,
    val imageUrl: String? = null
)

data class AiWordDetailsDto(
    val definition: String?,
    val example: String?,
    val synonyms: String?,
    val antonyms: String?,
    val baseForm: String?
)
