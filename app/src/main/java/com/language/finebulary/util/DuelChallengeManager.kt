package com.language.finebulary.util

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.DuelChallengePacket
import com.language.finebulary.data.model.Word
import com.language.finebulary.data.model.WordSnapshot
import com.language.finebulary.repository.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object DuelChallengeManager {
    private val gson: Gson = GsonBuilder().create()

    fun createPacket(
        courseName: String,
        words: List<Word>,
        creatorName: String = "Arkadaşın"
    ): DuelChallengePacket {
        val snapshots = words.map { word ->
            WordSnapshot(
                front = word.front,
                backsPacked = word.backsPacked,
                definition = word.definition,
                example = word.example
            )
        }
        return DuelChallengePacket(
            challengeId = UUID.randomUUID().toString().take(8),
            courseName = courseName,
            creatorName = creatorName,
            createdAtEpochMs = System.currentTimeMillis(),
            seed = System.currentTimeMillis() % 1000000L,
            words = snapshots
        )
    }

    fun encodePacketToJson(packet: DuelChallengePacket): String {
        return gson.toJson(packet)
    }

    fun decodePacketFromJson(json: String): DuelChallengePacket? {
        return try {
            gson.fromJson(json, DuelChallengePacket::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun encodePacketToUrlData(packet: DuelChallengePacket): String {
        val json = encodePacketToJson(packet)
        return try {
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).use { it.write(json.toByteArray(StandardCharsets.UTF_8)) }
            "z_" + Base64.encodeToString(bos.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        } catch (e: Exception) {
            Base64.encodeToString(json.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }

    fun decodePacketFromUrlData(urlData: String): DuelChallengePacket? {
        return try {
            val raw = urlData.trim()
            if (raw.startsWith("z_")) {
                val clean = raw.removePrefix("z_")
                val bytes = Base64.decode(clean, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val bis = ByteArrayInputStream(bytes)
                val json = GZIPInputStream(bis).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                decodePacketFromJson(json)
            } else {
                val bytes = Base64.decode(raw, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val json = String(bytes, StandardCharsets.UTF_8)
                decodePacketFromJson(json)
            }
        } catch (e: Exception) {
            decodePacketFromJson(urlData)
        }
    }

    suspend fun shortenUrl(longUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://tinyurl.com/api-create.php?url=" + java.net.URLEncoder.encode(longUrl, "UTF-8")
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val shortUrl = conn.inputStream.bufferedReader().readText().trim()
                if (shortUrl.startsWith("http")) shortUrl else longUrl
            } else {
                longUrl
            }
        } catch (e: Exception) {
            longUrl
        }
    }

    suspend fun saveChallengeToLibrary(
        repository: CourseRepository,
        packet: DuelChallengePacket
    ): Long {
        val courseName = "${packet.courseName} (Düello)"
        val courseId = repository.addCourseObject(
            Course(
                name = courseName,
                isPersonal = false
            )
        )
        val words = packet.words.map { snapshot ->
            Word(
                courseId = courseId,
                front = snapshot.front,
                backsPacked = snapshot.backsPacked,
                definition = snapshot.definition,
                example = snapshot.example
            )
        }
        repository.addWords(words)
        return courseId
    }
}
