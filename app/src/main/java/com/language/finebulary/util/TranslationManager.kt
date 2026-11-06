package com.language.finebulary.util

import android.util.Log
import com.google.gson.JsonParser
import com.language.finebulary.data.api.DictionaryApiService
import com.language.finebulary.data.api.GoogleTranslateApiService
import com.language.finebulary.data.api.MyMemoryApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class RichWordDetails(
    val front: String? = null,
    val translation: String?,
    val definition: String?,
    val synonyms: String?,
    val antonyms: String? = null,
    val example: String? = null,
    val imageUrl: String? = null,
    val suggestedCorrection: String? = null,
    val baseForm: String? = null,
    val baseFormTranslation: String? = null
)

object TranslationManager {

    private const val TAG = "TranslationManager"
    private const val GOOGLE_URL = "https://translate.googleapis.com/"
    private const val MEMORY_URL = "https://api.mymemory.translated.net/"
    private const val DICTIONARY_URL = "https://api.dictionaryapi.dev/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val memoryApi: MyMemoryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(MEMORY_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MyMemoryApiService::class.java)
    }

    private val googleApi: GoogleTranslateApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GOOGLE_URL)
            .client(client)
            .build()
            .create(GoogleTranslateApiService::class.java)
    }

    private val dictionaryApi: DictionaryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DICTIONARY_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DictionaryApiService::class.java)
    }

    private val datamuseApi: com.language.finebulary.data.api.DatamuseApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.datamuse.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.language.finebulary.data.api.DatamuseApiService::class.java)
    }

    suspend fun translate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): String? {
        // If source/target language are the same (Monolingual L2 -> L2 learning), return definition & synonyms instead of translating
        if (sourceLangCode.lowercase() == targetLangCode.lowercase()) {
            val details = getDictionaryDetails(text, targetLangCode, targetLangCode, "WIKTIONARY")
            val def = details.definition
            val syns = details.synonyms?.replace("|", ", ")
            return when {
                !def.isNullOrBlank() && !syns.isNullOrBlank() -> "$def (Syn: $syns)"
                !def.isNullOrBlank() -> def
                !syns.isNullOrBlank() -> "Synonyms: $syns"
                else -> text
            }
        }

        // Try Google Translate first (Very fast, no limits)
        val googleResult = googleTranslate(text, sourceLangCode, targetLangCode)
        if (!googleResult.isNullOrBlank()) {
            return googleResult
        }

        // Fallback to MyMemory if Google fails
        return myMemoryTranslate(text, sourceLangCode, targetLangCode)
    }

    private suspend fun googleTranslate(text: String, sl: String, tl: String): String? {
        Log.d(TAG, "Fetching translation from Google API for: $text")
        return try {
            val response = googleApi.translate(target = tl, source = sl, text = text)
            val jsonString = response.string()
            val outerArray = JsonParser.parseString(jsonString).asJsonArray
            val innerArray = outerArray.get(0).asJsonArray
            
            val sb = StringBuilder()
            for (i in 0 until innerArray.size()) {
                sb.append(innerArray.get(i).asJsonArray.get(0).asString)
            }
            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Google API Exception: ${e.message}")
            null
        }
    }

    private suspend fun myMemoryTranslate(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): String? {
        val langPair = "$sourceLangCode|$targetLangCode"
        Log.d(TAG, "Fetching translation from MyMemory API for: $text ($langPair)")
        return try {
            val response = memoryApi.translate(text, langPair)
            if (response.responseStatus == 200 || response.responseStatus.toString().startsWith("2")) {
                val translated = response.responseData.translatedText
                if (translated.contains("MYMEMORY WARNING", ignoreCase = true)) {
                    Log.e(TAG, "MyMemory Quota Exceeded: $translated")
                    null
                } else {
                    Log.d(TAG, "MyMemory API Success: $translated")
                    translated
                }
            } else {
                Log.e(TAG, "MyMemory API Error. Status: ${response.responseStatus}")
                if (response.responseStatus == 403 || response.responseStatus == 429) {
                    Log.e(TAG, "MyMemory API Limit Reached or Forbidden")
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "MyMemory API Exception: ${e.message}", e)
            null
        }
    }

    suspend fun getDictionaryDetails(
        text: String,
        targetLangCode: String, 
        nativeLangCode: String,
        exampleSource: String = "WIKTIONARY"
    ): RichWordDetails = coroutineScope {
        Log.d(TAG, "getDictionaryDetails triggered for: $text (exampleSource: $exampleSource)")
        
        // Fast Source (Datamuse) - High reliability for speed
        val datamuseDeferred = async {
            try {
                datamuseApi.getDetails(text).firstOrNull()
            } catch (e: Exception) {
                Log.e(TAG, "Datamuse Details Exception: ${e.message}")
                null
            }
        }

        // Slow Source (DictionaryAPI) - Only if exampleSource is WIKTIONARY
        val slowDictionaryDeferred = async {
            if (exampleSource == "NONE") return@async null
            try {
                dictionaryApi.getDetails(targetLangCode, text).firstOrNull()
            } catch (_: Exception) {
                null
            }
        }

        // Fast Source (Datamuse Antonyms)
        val datamuseAntonymsDeferred = async {
            try {
                datamuseApi.getAntonyms(text).map { it.word }
            } catch (_: Exception) {
                emptyList()
            }
        }

        val datamuseEntry = datamuseDeferred.await()
        val slowEntry = slowDictionaryDeferred.await()
        val datamuseAnts = datamuseAntonymsDeferred.await()

        var baseForm: String? = null
        var baseFormTranslation: String? = null

        // 1. Logic for Base Form
        val activeEntryWord = slowEntry?.word ?: datamuseEntry?.word
        if (activeEntryWord != null && activeEntryWord.lowercase() != text.lowercase()) {
            baseForm = activeEntryWord
            baseFormTranslation = translate(baseForm, targetLangCode, nativeLangCode)
        }

        // 2. Logic for Definition (Prefer slow detailed, fallback to fast Datamuse)
        val definition = slowEntry?.meanings?.firstOrNull()?.definitions?.firstOrNull()?.definition
            ?: datamuseEntry?.defs?.firstOrNull()?.let { 
                // Datamuse defs come like "n\tdefinition", clean it up
                if (it.contains("\t")) it.substringAfter("\t") else it
            }

        // 3. Logic for Example (Only if dictionary provides a valid one)
        val example = slowEntry?.meanings?.flatMap { it.definitions }
            ?.firstOrNull { !it.example.isNullOrBlank() }?.example

        // 4. Logic for Synonyms
        var synonyms: String? = null
        val foundSyns = slowEntry?.meanings?.flatMap { it.synonyms ?: emptyList() }?.distinct()?.take(5)
        if (!foundSyns.isNullOrEmpty()) {
            synonyms = foundSyns.joinToString("|")
        }

        // 5. Logic for Antonyms
        val slowAnts = slowEntry?.meanings?.flatMap { (it.antonyms ?: emptyList()) + it.definitions.flatMap { def -> def.antonyms ?: emptyList() } } ?: emptyList()
        val combinedAnts = (slowAnts + datamuseAnts).distinct().take(5)
        var antonyms: String? = null
        if (combinedAnts.isNotEmpty()) {
            antonyms = combinedAnts.joinToString("|")
        }

        RichWordDetails(
            front = null,
            translation = null,
            definition = definition,
            synonyms = synonyms,
            antonyms = antonyms,
            example = example,
            baseForm = baseForm,
            baseFormTranslation = baseFormTranslation
        )
    }

    suspend fun getSpellingSuggestion(text: String): String? {
        return try {
            val suggestions = datamuseApi.getSuggestions(text)
            // If the first suggestion is different from input and has a high score, suggest it
            val best = suggestions.firstOrNull()
            if (best != null && best.word.lowercase() != text.lowercase() && best.score > 500) {
                best.word
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Datamuse API error: ${e.message}")
            null
        }
    }
}
