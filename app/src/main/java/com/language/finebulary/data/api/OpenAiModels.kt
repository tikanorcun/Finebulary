package com.language.finebulary.data.api

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val response_format: OpenAiResponseFormat? = null
)

data class OpenAiMessage(
    val role: String,
    val content: String
)

data class OpenAiResponseFormat(
    val type: String = "json_object"
)

data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>
)

data class OpenAiChoice(
    val message: OpenAiMessage
)

data class OpenAiModelListResponse(
    val data: List<OpenAiModelInfo>
)

data class OpenAiModelInfo(
    val id: String
)
