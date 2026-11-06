package com.language.finebulary.util

import com.language.finebulary.data.api.GeminiApiService
import com.language.finebulary.data.api.OpenAiApiService
import javax.inject.Inject
import javax.inject.Singleton

enum class AiServiceProvider {
    GEMINI, GROQ, OPENAI, DEEPSEEK, OPENROUTER
}

@Singleton
class ProviderRegistry @Inject constructor(
    private val geminiApi: GeminiApiService,
    private val openAiApi: OpenAiApiService
) {
    fun getProvider(provider: AiServiceProvider, showLegacyModels: Boolean = false): AiProvider {
        return when (provider) {
            AiServiceProvider.GEMINI -> GeminiProvider(geminiApi, showLegacyModels)
            AiServiceProvider.GROQ -> OpenAiCompatibleProvider(openAiApi, "https://api.groq.com/openai/", "Groq")
            AiServiceProvider.OPENAI -> OpenAiCompatibleProvider(openAiApi, "https://api.openai.com/", "OpenAI")
            AiServiceProvider.DEEPSEEK -> OpenAiCompatibleProvider(openAiApi, "https://api.deepseek.com/", "DeepSeek")
            AiServiceProvider.OPENROUTER -> OpenAiCompatibleProvider(openAiApi, "https://openrouter.ai/api/", "OpenRouter")
        }
    }
}
