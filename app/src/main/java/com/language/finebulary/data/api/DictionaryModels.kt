package com.language.finebulary.data.api

import com.google.gson.annotations.SerializedName

data class DictionaryEntry(
    @SerializedName("word")
    val word: String,
    @SerializedName("meanings")
    val meanings: List<Meaning>
)

data class Meaning(
    @SerializedName("partOfSpeech")
    val partOfSpeech: String,
    @SerializedName("definitions")
    val definitions: List<Definition>,
    @SerializedName("synonyms")
    val synonyms: List<String>? = null,
    @SerializedName("antonyms")
    val antonyms: List<String>? = null
)

data class Definition(
    @SerializedName("definition")
    val definition: String,
    @SerializedName("synonyms")
    val synonyms: List<String>? = null,
    @SerializedName("antonyms")
    val antonyms: List<String>? = null,
    @SerializedName("example")
    val example: String?
)
