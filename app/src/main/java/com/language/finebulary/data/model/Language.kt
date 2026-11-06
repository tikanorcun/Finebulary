package com.language.finebulary.data.model

import com.language.finebulary.R

data class Language(
    val nameRes: Int,
    val code: String
)

val supportedLanguages = listOf(
    Language(R.string.lang_english, "en"),
    Language(R.string.lang_turkish, "tr"),
    Language(R.string.lang_spanish, "es"),
    Language(R.string.lang_french, "fr"),
    Language(R.string.lang_german, "de"),
    Language(R.string.lang_italian, "it"),
    Language(R.string.lang_russian, "ru"),
    Language(R.string.lang_chinese, "zh"),
    Language(R.string.lang_japanese, "ja"),
    Language(R.string.lang_korean, "ko"),
    Language(R.string.lang_portuguese, "pt"),
    Language(R.string.lang_arabic, "ar"),
    Language(R.string.lang_dutch, "nl"),
    Language(R.string.lang_greek, "el"),
    Language(R.string.lang_hindi, "hi")
)
