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

    fun speak(
        text: String,
        langCode: String = "en",
        isSlow: Boolean = false,
        speechRate: Float? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized) {
            onDone?.invoke()
            return
        }
        
        val locale = Locale(langCode)
        tts?.language = locale
        val rate = speechRate ?: if (isSlow) 0.5f else 1.0f
        tts?.setSpeechRate(rate)

        if (onDone != null) {
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onDone()
                }
                @Suppress("DEPRECATION")
                override fun onError(utteranceId: String?) {
                    onDone()
                }
            })
        }

        // Request highest quality network/neural voice synthesis if available on device
        val params = Bundle().apply {
            putString("networkSynthesis", "true")
            putString("quality", "high")
        }
        
        val utteranceId = "FinebularyUtterance_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
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
