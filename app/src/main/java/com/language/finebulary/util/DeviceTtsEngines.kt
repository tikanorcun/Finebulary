package com.language.finebulary.util

import android.content.Context
import android.speech.tts.TextToSpeech

data class TtsEngineInfo(
    val packageName: String,
    val label: String
)

object DeviceTtsEngines {
    fun getInstalledEngines(context: Context): List<TtsEngineInfo> {
        return try {
            var engines: List<TtsEngineInfo> = emptyList()
            var tempTts: TextToSpeech? = null
            tempTts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        engines = tempTts?.engines?.map { 
                            TtsEngineInfo(packageName = it.name, label = it.label)
                        } ?: emptyList()
                    } catch (e: Exception) {}
                }
            }
            Thread.sleep(200)
            if (engines.isEmpty()) {
                engines = tempTts.engines?.map { 
                    TtsEngineInfo(packageName = it.name, label = it.label)
                } ?: emptyList()
            }
            tempTts?.shutdown()
            engines
        } catch (e: Exception) {
            emptyList()
        }
    }
}
