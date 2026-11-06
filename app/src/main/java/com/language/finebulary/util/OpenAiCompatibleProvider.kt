package com.language.finebulary.util

import android.util.Log
import com.google.gson.Gson
import com.language.finebulary.data.api.*
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenAiCompatibleProvider(
    private val apiService: OpenAiApiService,
    private val baseUrl: String,
    private val providerName: String
) : AiProvider {
    private val gson = Gson()
    private val TAG = "OpenAiProvider($providerName)"

    override suspend fun fetchModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl}v1/models"
            val auth = "Bearer $apiKey"
            val response = apiService.listModels(url, auth)
            response.data
                .map { it.id }
                .filter { modelId ->
                    when (providerName) {
                        "OpenAI" -> modelId.startsWith("gpt-4") || modelId.startsWith("o1")
                        "Groq" -> modelId.contains("llama") || modelId.contains("mixtral") || modelId.contains("gemma")
                        "OpenRouter" -> {
                            modelId.startsWith("google/") || 
                            modelId.startsWith("anthropic/") || 
                            modelId.startsWith("openai/") || 
                            modelId.startsWith("meta-llama/") ||
                            modelId.startsWith("mistralai/") ||
                            modelId.startsWith("deepseek/")
                        }
                        else -> true
                    }
                }
                .sorted()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch models: ${e.message}")
            emptyList()
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
        val url = "${baseUrl}v1/chat/completions"
        val auth = "Bearer $apiKey"
        
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

        val request = OpenAiChatRequest(
            model = modelId,
            messages = listOf(
                OpenAiMessage("system", "You are a helpful language teacher. You always respond in valid JSON."),
                OpenAiMessage("user", prompt)
            ),
            response_format = OpenAiResponseFormat("json_object")
        )

        try {
            val response = apiService.chatCompletion(url, auth, request)
            val jsonText = response.choices.firstOrNull()?.message?.content ?: throw Exception("Empty AI response")
            
            val cleanedJson = JsonCleaner.clean(jsonText)
            val generatedData = gson.fromJson(cleanedJson, AiGeneratedCourse::class.java)
            
            if (generatedData?.words == null) {
                throw Exception("Failed to parse AI response")
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
            Log.e(TAG, "Generation failed: ${e.message}")
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
    ): com.language.finebulary.data.api.IeltsPassage = withContext(Dispatchers.IO) {
        val url = "${baseUrl}v1/chat/completions"
        val auth = "Bearer $apiKey"
        
        val prompt = """
            Create an engaging, high-quality reading passage or story for CEFR Level $level.
            $topicPrompt
            Target Language: $targetLang. Native Language (for keywords/explanations): $nativeLang.
            
            Passage Requirements:
            - Length: 300-500 words.
            - Structure: Write as a continuous, flowing text or natural story paragraphs (Do NOT use Section A, Section B markers unless it is an academic text).
            
            Question Requirements (10-12 questions in total):
            - Group 1: HEADING_MATCHING (Match sections A, B, C etc. to headings).
            - Group 2: TFNG (TRUE/FALSE/NOT GIVEN).
            - Group 3: GAP_FILL (Sentence completion).
            - Group 4: MULTIPLE_CHOICE (Traditional academic questions).
            
            STRICT ORDER: HEADING_MATCHING -> TFNG -> GAP_FILL -> MULTIPLE_CHOICE.
            
            Return a JSON object:
            {
              "title": "A catchy academic title",
              "passage": "The full academic text in $targetLang with Section markers.",
              "translatedPassage": "The exact same text translated naturally into $nativeLang.",
              "questions": [
                {
                  "type": "HEADING_MATCHING",
                  "question": "Which heading matches Section A?",
                  "options": ["Heading 1", "Heading 2", "Heading 3", "Heading 4", "Heading 5"],
                  "correctIndex": 0,
                  "explanation": "Why this heading fits in $nativeLang"
                },
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

        val request = OpenAiChatRequest(
            model = modelId,
            messages = listOf(
                OpenAiMessage("system", "You are an IELTS examiner. Respond only in valid JSON."),
                OpenAiMessage("user", prompt)
            ),
            response_format = OpenAiResponseFormat("json_object")
        )

        try {
            val response = apiService.chatCompletion(url, auth, request)
            val jsonText = response.choices.firstOrNull()?.message?.content ?: throw Exception("Empty response")
            val cleanedJson = JsonCleaner.clean(jsonText)
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
        val url = "${baseUrl}v1/chat/completions"
        val auth = "Bearer $apiKey"
        
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

        val request = OpenAiChatRequest(
            model = modelId,
            messages = listOf(
                OpenAiMessage("system", "You are a helpful dictionary assistant. You always respond in valid JSON."),
                OpenAiMessage("user", prompt)
            ),
            response_format = OpenAiResponseFormat("json_object")
        )

        try {
            val response = apiService.chatCompletion(url, auth, request)
            val jsonText = response.choices.firstOrNull()?.message?.content ?: throw Exception("Empty AI response")
            val cleanedJson = JsonCleaner.clean(jsonText)
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
