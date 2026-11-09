package com.language.finebulary.repository

import com.language.finebulary.data.dao.VocabularyDao
import com.language.finebulary.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.max
import kotlin.math.min

class CourseRepository(private val vocabularyDao: VocabularyDao) {

    val allCourses: Flow<List<Course>> = vocabularyDao.getAllCourses()
    val allCoursesWithCount: Flow<List<CourseWithCount>> = vocabularyDao.getAllCoursesWithCount()

    suspend fun importStarterDeck(deck: StarterDeck): Long {
        val courseId = vocabularyDao.insertCourse(
            Course(
                name = deck.name,
                isPersonal = false,
                nativeLanguageCode = deck.nativeLang,
                targetLanguageCode = deck.targetLang
            )
        )
        val words = deck.words.map { sw ->
            Word(
                courseId = courseId,
                front = sw.front,
                backsPacked = sw.back,
                definition = sw.definition,
                example = sw.example
            )
        }
        addWords(words)
        return courseId
    }

    suspend fun addCourse(name: String): Long {
        return vocabularyDao.insertCourse(Course(name = name))
    }

    suspend fun addCourseObject(course: Course): Long {
        return vocabularyDao.insertCourse(course)
    }

    suspend fun addWords(words: List<Word>) {
        val wordIds = vocabularyDao.insertWords(words)
        wordIds.forEach { id ->
            vocabularyDao.insertReviewState(ReviewState(wordId = id))
        }
    }

    suspend fun addSingleWord(
        courseId: Long, 
        front: String, 
        backsPacked: String,
        definition: String? = null,
        synonyms: String? = null,
        antonyms: String? = null,
        example: String? = null,
        imageUrl: String? = null
    ): Boolean {
        val existingWords = vocabularyDao.getAllWordsRaw().filter { it.courseId == courseId }
        val trimmedFront = front.trim()
        if (existingWords.any { it.front.trim().equals(trimmedFront, ignoreCase = true) }) {
            return false
        }
        val wordId = vocabularyDao.insertWord(
            Word(
                courseId = courseId, 
                front = trimmedFront, 
                backsPacked = backsPacked,
                definition = definition,
                synonyms = synonyms,
                antonyms = antonyms,
                example = example,
                imageUrl = imageUrl
            )
        )
        vocabularyDao.insertReviewState(ReviewState(wordId = wordId))
        return true
    }

    suspend fun updateWord(word: Word) {
        vocabularyDao.updateWord(word)
    }

    suspend fun deleteWord(word: Word) {
        vocabularyDao.deleteReviewStateForWord(word.id)
        vocabularyDao.deleteWord(word)
    }

    suspend fun deleteWords(words: List<Word>) {
        val ids = words.map { it.id }
        vocabularyDao.deleteReviewStatesForWords(ids)
        vocabularyDao.deleteWords(words)
    }

    suspend fun getAllWordsRaw() = vocabularyDao.getAllWordsRaw()
    suspend fun getWordsForCourseIds(courseIds: List<Long>) = vocabularyDao.getWordsForCourseIds(courseIds)
    suspend fun getAllReviewStatesRaw() = vocabularyDao.getAllReviewStatesRaw()

    suspend fun importFullData(courses: List<Course>, words: List<Word>, states: List<ReviewState>) {
        vocabularyDao.insertCourses(courses)
        vocabularyDao.insertWords(words)
        vocabularyDao.insertReviewStates(states)
    }

    suspend fun getOrCreateCourse(name: String): Long {
        val existing = vocabularyDao.getCourseByName(name)
        return existing?.id ?: vocabularyDao.insertCourse(Course(name = name))
    }

    suspend fun getCourseById(courseId: Long): Course? {
        return vocabularyDao.getCourseById(courseId)
    }

    fun getWordsForCourse(courseId: Long) = vocabularyDao.getWordsForCourse(courseId)

    fun getDailyProgress(date: String): Flow<DailyProgress?> = vocabularyDao.getDailyProgress(date)

    suspend fun incrementDailyProgress(date: String) {
        val existing = vocabularyDao.getDailyProgress(date).first()
        if (existing == null) {
            vocabularyDao.insertDailyProgress(DailyProgress(date = date, wordsLearned = 1))
        } else {
            vocabularyDao.incrementDailyProgress(date)
        }
    }

    fun getDueWords(courseId: Long) = vocabularyDao.getDueWords(courseId, System.currentTimeMillis())

    fun getAllIeltsPassages(): Flow<List<IeltsPassageEntity>> = vocabularyDao.getAllIeltsPassages()
    suspend fun addIeltsPassage(passage: IeltsPassageEntity): Long = vocabularyDao.insertIeltsPassage(passage)
    suspend fun updateIeltsPassage(passage: IeltsPassageEntity) = vocabularyDao.updateIeltsPassage(passage)
    suspend fun deleteIeltsPassage(passage: IeltsPassageEntity) = vocabularyDao.deleteIeltsPassage(passage)
    suspend fun getIeltsPassageRaw(id: Long) = vocabularyDao.getIeltsPassageRaw(id)

    suspend fun getTotalDueWordsCount(): Int = vocabularyDao.getTotalDueWordsCount(System.currentTimeMillis())

    suspend fun getRandomDistractors(excludeIds: List<Long>, nativeLanguageCode: String, count: Int): List<String> {
        return vocabularyDao.getRandomWordsByLanguage(excludeIds, nativeLanguageCode, count).map { 
            it.backsPacked.split("|").first().trim() 
        }
    }

    suspend fun getReviewState(wordId: Long): ReviewState? {
        return vocabularyDao.getReviewStateForWord(wordId)
    }

    suspend fun deleteCourse(course: Course) {
        vocabularyDao.deleteReviewStatesForCourse(course.id)
        vocabularyDao.deleteWordsForCourse(course.id)
        vocabularyDao.deleteCourse(course)
    }

    suspend fun restartCourse(courseId: Long) {
        val course = vocabularyDao.getCourseById(courseId)
        if (course != null) {
            vocabularyDao.updateCourse(course.copy(currentStreak = 0, lastReviewDay = 0))
        }
        vocabularyDao.deleteReviewStatesForCourse(courseId)
        val words = vocabularyDao.getWordsForCourse(courseId).first()
        words.forEach { word ->
            vocabularyDao.insertReviewState(ReviewState(wordId = word.id))
        }
    }

    suspend fun resetAllProgress() {
        vocabularyDao.deleteAllReviewStates()
        // Re-initialize review states for all existing words
        val words = vocabularyDao.getRandomWordsByLanguage(emptyList(), "en", 100000) // Get all
        words.forEach { word ->
            vocabularyDao.insertReviewState(ReviewState(wordId = word.id))
        }
    }

    suspend fun wipeAllData() {
        vocabularyDao.deleteAllReviewStates()
        vocabularyDao.deleteAllWords()
        vocabularyDao.deleteAllCourses()
    }

    suspend fun updateReview(courseId: Long, wordId: Long, rating: String) {
        val existingState = vocabularyDao.getReviewStateForWord(wordId)
        val state = existingState ?: ReviewState(wordId = wordId)

        var newEase = state.ease
        var newInterval = state.intervalDays
        val newReps = state.reps + 1
        var newLapses = state.lapses

        when (rating) {
            "HARD" -> {
                newLapses += 1
                newInterval = 1
                newEase = max(1.3, newEase - 0.2)
            }
            "GOOD" -> {
                // Growth: Exponential factor based on Ease
                newInterval = if (newInterval == 0) 4 else max(4, (newInterval * newEase).toInt())
            }
            "EASY" -> {
                // Massive jump for easy words
                newInterval = if (newInterval == 0) 10 else max(10, (newInterval * newEase * 1.5).toInt())
                newEase = min(2.8, newEase + 0.15)
            }
        }

        // Mastery Cap: Max interval of 365 days
        newInterval = min(365, newInterval)

        val nextDueDate = System.currentTimeMillis() + (newInterval * 24L * 60 * 60 * 1000)

        val updatedState = state.copy(
            ease = newEase,
            intervalDays = newInterval,
            dueDateEpochMs = nextDueDate,
            reps = newReps,
            lapses = newLapses,
            lastReviewedAtEpochMs = System.currentTimeMillis()
        )

        vocabularyDao.insertReviewState(updatedState)
        updateStreak(courseId)
    }

    suspend fun updateCourse(course: Course) {
        vocabularyDao.updateCourse(course)
    }

    private suspend fun updateStreak(courseId: Long) {
        val course = vocabularyDao.getCourseById(courseId) ?: return
        val currentDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
        
        val lastDay = course.lastReviewDay
        var newStreak = course.currentStreak

        if (lastDay == currentDay) {
            return
        } else if (lastDay == currentDay - 1) {
            newStreak += 1
        } else {
            newStreak = 1
        }

        vocabularyDao.updateCourse(course.copy(currentStreak = newStreak, lastReviewDay = currentDay))
    }
}
