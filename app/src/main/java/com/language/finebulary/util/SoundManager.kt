package com.language.finebulary.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object SoundManager {
    private var toneGenerator: ToneGenerator? = null

    fun init() {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            } catch (e: Exception) {
                // Silently ignore if audio hardware is unavailable
            }
        }
    }

    fun playSuccess() {
        // A pleasant two-tone "Ding-Dong" or "Success" chime
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 80)
            Thread {
                Thread.sleep(100)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_3, 120)
            }.start()
        } catch (e: Exception) {}
    }

    fun playError() {
        // A softer, non-aggressive double pulse
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 200)
        } catch (e: Exception) {}
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
