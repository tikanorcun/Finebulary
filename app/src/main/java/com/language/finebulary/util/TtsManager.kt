package com.language.finebulary.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

object TtsManager {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun init(context: Context, enginePackage: String? = null) {
        if (tts != null) {
            tts?.shutdown()
            tts = null
            isInitialized = false
        }
        
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
            } else {
                Log.e("TtsManager", "Initialization failed")
            }
        }, enginePackage)
    }

    fun speak(text: String, langCode: String = "en", isSlow: Boolean = false) {
        if (!isInitialized) return
        
        val locale = Locale(langCode)
        tts?.language = locale
        val rate = if (isSlow) 0.5f else 1.0f
        tts?.setSpeechRate(rate)

        // Request highest quality network/neural voice synthesis if available on device
        val params = Bundle().apply {
            putString("networkSynthesis", "true")
            putString("quality", "high")
        }
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "FinebularyUtteranceId")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
