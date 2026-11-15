package com.language.finebulary.data.model

data class WordSnapshot(
    val front: String,
    val backsPacked: String,
    val definition: String? = null,
    val example: String? = null
)

data class DuelChallengePacket(
    val challengeId: String,
    val courseName: String,
    val creatorName: String = "Arkadaşın",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val seed: Long = System.currentTimeMillis(),
    val words: List<WordSnapshot>
)
