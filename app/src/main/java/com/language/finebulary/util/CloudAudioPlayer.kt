package com.language.finebulary.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object CloudAudioPlayer {
    private const val TAG = "CloudAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null

    suspend fun playSpeech(
        context: Context,
        text: String,
        provider: String, // "OPENAI", "ELEVENLABS", "AZURE", "DEEPGRAM", "STANDARD"
        apiKey: String,
        langCode: String = "en",
        azureRegion: String = "eastus"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            stop()
            val client = OkHttpClient.Builder().build()
            val tempFile = File(context.cacheDir, "speech_temp.mp3")

            when (provider) {
                "OPENAI" -> {
                    val json = JSONObject().apply {
                        put("model", "tts-1")
                        put("input", text)
                        put("voice", "alloy")
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url("https://api.openai.com/v1/audio/speech")
                        .header("Authorization", "Bearer $apiKey")
                        .post(body)
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) return@withContext false
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                }
                "ELEVENLABS" -> {
                    val voiceId = "21m00Tcm4TlvDq8ikWAM" // Rachel
                    val json = JSONObject().apply {
                        put("text", text)
                        put("model_id", "eleven_multilingual_v2")
                    }
                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                        .header("xi-api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .post(body)
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) return@withContext false
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                }
                "AZURE" -> {
                    val ssml = "<speak version='1.0' xml:lang='$langCode'><voice name='en-US-AriaNeural'>$text</voice></speak>"
                    val body = ssml.toRequestBody("application/ssml+xml; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url("https://$azureRegion.tts.speech.microsoft.com/cognitiveservices/v1")
                        .header("Ocp-Apim-Subscription-Key", apiKey)
                        .header("X-Microsoft-OutputFormat", "audio-16khz-32kbitrate-mono-mp3")
                        .header("Content-Type", "application/ssml+xml")
                        .post(body)
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) return@withContext false
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                }
                "DEEPGRAM" -> {
                    val request = Request.Builder()
                        .url("https://api.deepgram.com/v1/speak?model=aura-asteria-en")
                        .header("Authorization", "Token $apiKey")
                        .header("Content-Type", "application/json")
                        .post("{\"text\":\"$text\"}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) return@withContext false
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                }
                "STANDARD" -> {
                    return@withContext false
                }
                else -> return@withContext false
            }

            withContext(Dispatchers.Main) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        release()
                        mediaPlayer = null
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "CloudAudioPlayer error: ${e.message}", e)
            false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {}
    }
}
