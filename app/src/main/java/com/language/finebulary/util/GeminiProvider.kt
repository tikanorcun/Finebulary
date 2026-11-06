package com.language.finebulary.util

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.language.finebulary.data.api.AiGeneratedCourse
import com.language.finebulary.data.api.GeminiApiService
import com.language.finebulary.data.api.IeltsPassage
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiProvider(
    private val geminiApi: GeminiApiService,
    private val showLegacyModels: Boolean = false
) : AiProvider {
    private val gson = Gson()
    private val TAG = "GeminiProvider"

    override suspend fun fetchModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = geminiApi.listModels(apiKey)
            response.models
                .filter { it.supportedGenerationMethods.contains("generateContent") }
                .filter { modelInfo ->
                    if (showLegacyModels) true
                    else {
                        val name = modelInfo.name.removePrefix("models/")
                        val versionMatch = Regex("""gemini-(\d+)\.""").find(name)
                        val version = versionMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        version >= 3
                    }
                }
                .map { it.name }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch models: ${e.message}")
            listOf("models/gemini-3-flash")
        }
    }

    override suspend fun generateCourse(
        apiKey: String,
        modelId: String,
        topic: String,
        targetLang: String,
        nativeLang: String,
        wordCount: Int,
        level: String
    ): Pair<Course, List<Word>> = withContext(Dispatchers.IO) {
        val cleanModelName = modelId.removePrefix("models/")
        val generativeModel = GenerativeModel(
            modelName = cleanModelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
            )
        )

        val prompt = """
            Generate a language learning course for the topic: $topic at CEFR Level $level.
            The target language is $targetLang and the explanation language is $nativeLang.
            ${if (targetLang.lowercase() == nativeLang.lowercase()) "Note: This is a monolingual (L2 to L2) course. The 'back' and 'definition' fields must provide clear definitions, explanations, and synonyms in $targetLang rather than translations." else ""}
            The course should contain exactly $wordCount high-quality, essential words or phrases suitable for $level level.
            Return a JSON object matching this schema:
            {
              "name": "Title of the course",
              "words": [
                {
                  "front": "Word in $targetLang",
                  "back": "Meaning/Definition in $nativeLang",
                  "definition": "Simple definition in $nativeLang",
                  "example": "Example sentence using the word in $targetLang"
                }
              ]
            }
            Ensure the JSON is valid and only return the JSON content.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val cleanedJson = JsonCleaner.clean(response.text)
            val generatedData = gson.fromJson(cleanedJson, AiGeneratedCourse::class.java)
            
            if (generatedData?.words == null) {
                throw Exception("Failed to parse AI response into words list.")
            }

            val course = Course(
                name = generatedData.name,
                isPersonal = false,
                nativeLanguageCode = nativeLang,
                targetLanguageCode = targetLang
            )
            
            val words = generatedData.words.map { sw ->
                Word(
                    front = sw.front,
                    backsPacked = sw.back,
                    definition = sw.definition,
                    example = sw.example,
                    courseId = 0 
                )
            }
            
            Pair(course, words)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Generation failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun generateIeltsReading(
        apiKey: String,
        modelId: String,
        targetLang: String,
        nativeLang: String,
        level: String,
        topicPrompt: String?
    ): IeltsPassage = withContext(Dispatchers.IO) {
        val cleanModelName = modelId.removePrefix("models/")
        val generativeModel = GenerativeModel(
            modelName = cleanModelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )

        val prompt = """
            Create an engaging, high-quality reading text for CEFR Level $level.
            $topicPrompt
            Target Language: $targetLang. Native Language (for explanations): $nativeLang.
            
            Rules based on Category:
            - If the category is 'Hikaye' (Story): Write an engaging, natural story (like Aesop's fables, adventure, etc.). NO Section markers.
            - If the category is 'Güncel Makale' (Current Article) or 'Bilim ve Teknoloji' or 'Tarih': Write a natural, continuous article or narrative summary without section markers.
            - If the category is 'Akademik / Sınav' (Academic / Exam): Write a professional IELTS/TOEFL style passage. It can optionally include sections (Section A, Section B) or be continuous.
            
            Question Requirements (Total 5 questions):
            - Exactly 2 True/False questions (type: "TFNG", options: ["TRUE", "FALSE", "NOT GIVEN"]).
            - Exactly 2 Gap Fill sentence completion questions (type: "GAP_FILL", correctAnswer: "word").
            - Exactly 1 Multiple Choice question (type: "MULTIPLE_CHOICE").
            
            Return a JSON object:
            {
              "title": "A catchy title in $targetLang",
              "passage": "The full text in $targetLang.",
              "translatedPassage": "The exact same text translated naturally into $nativeLang.",
              "questions": [
                {
                  "type": "TFNG",
                  "question": "Statement to check against text",
                  "options": ["TRUE", "FALSE", "NOT GIVEN"],
                  "correctIndex": 0,
                  "explanation": "Explanation in $nativeLang"
                },
                {
                  "type": "GAP_FILL",
                  "question": "Sentence with a [BLANK] to fill",
                  "correctAnswer": "word",
                  "explanation": "Explanation in $nativeLang"
                },
                {
                  "type": "MULTIPLE_CHOICE",
                  "question": "Question text",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctIndex": 1,
                  "explanation": "Why this is correct in $nativeLang"
                }
              ],
              "academicKeywords": [
                { "front": "word", "back": "translation", "definition": "simple definition", "example": "sentence" }
              ]
            }
            Ensure the JSON is valid and only return the JSON content.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val cleanedJson = JsonCleaner.clean(response.text)
            gson.fromJson(cleanedJson, IeltsPassage::class.java)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun generateWordDetails(
        apiKey: String,
        modelId: String,
        word: String,
        targetLang: String,
        nativeLang: String
    ): RichWordDetails = withContext(Dispatchers.IO) {
        val cleanModelName = modelId.removePrefix("models/")
        val generativeModel = GenerativeModel(
            modelName = cleanModelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
            )
        )

        val prompt = """
            Provide dictionary details for the word/phrase "$word" in target language $targetLang, explained in native language $nativeLang.
            Return a JSON object matching this schema:
            {
              "definition": "Clear definition in $nativeLang",
              "example": "A natural example sentence using the word in $targetLang",
              "synonyms": "synonym1|synonym2|synonym3 (or null if none)",
              "antonyms": "antonym1|antonym2|antonym3 (or null if none)",
              "baseForm": "Base form of the word if inflected (or null)"
            }
            Ensure the JSON is valid and only return the JSON content.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val cleanedJson = JsonCleaner.clean(response.text)
            val data = gson.fromJson(cleanedJson, com.language.finebulary.data.api.AiWordDetailsDto::class.java)
            RichWordDetails(
                front = word,
                translation = null,
                definition = data?.definition,
                synonyms = data?.synonyms,
                antonyms = data?.antonyms,
                example = data?.example,
                baseForm = data?.baseForm
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate word details: ${e.message}")
            RichWordDetails(front = word, translation = null, definition = null, synonyms = null, example = null)
        }
    }
}
