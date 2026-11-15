package com.language.finebulary.util

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.DuelChallengePacket
import com.language.finebulary.data.model.Word
import com.language.finebulary.data.model.WordSnapshot
import com.language.finebulary.repository.CourseRepository
import java.nio.charset.StandardCharsets
import java.util.UUID

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
        return Base64.encodeToString(json.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun decodePacketFromUrlData(urlData: String): DuelChallengePacket? {
        return try {
            val bytes = Base64.decode(urlData, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val json = String(bytes, StandardCharsets.UTF_8)
            decodePacketFromJson(json)
        } catch (e: Exception) {
            // Fallback try raw json
            decodePacketFromJson(urlData)
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
