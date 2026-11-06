package com.language.finebulary.util

object JsonCleaner {
    fun clean(rawText: String?): String {
        if (rawText.isNullOrBlank()) {
            throw Exception("AI response text is null or empty")
        }

        var cleaned = rawText.trim()

        // Remove markdown code blocks if present
        if (cleaned.startsWith("```")) {
            val firstNewline = cleaned.indexOf('\n')
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1)
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length - 3)
            }
            cleaned = cleaned.trim()
        }

        // Also handle cases where markdown lacks newline after ```json
        cleaned = cleaned.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        // Extract JSON object or array bounds if extra text surrounds it
        val firstBrace = cleaned.indexOf('{')
        val firstBracket = cleaned.indexOf('[')
        
        val startIndex = when {
            firstBrace != -1 && firstBracket != -1 -> minOf(firstBrace, firstBracket)
            firstBrace != -1 -> firstBrace
            else -> firstBracket
        }

        val lastBrace = cleaned.lastIndexOf('}')
        val lastBracket = cleaned.lastIndexOf(']')
        
        val endIndex = maxOf(lastBrace, lastBracket)

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1)
        }

        return cleaned
    }
}
