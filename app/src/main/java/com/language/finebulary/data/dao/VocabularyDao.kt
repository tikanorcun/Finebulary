package com.language.finebulary.data.dao

import androidx.room.*
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.CourseWithCount
import com.language.finebulary.data.model.DailyProgress
import com.language.finebulary.data.model.IeltsPassageEntity
import com.language.finebulary.data.model.ReviewState
import com.language.finebulary.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    // Courses
    @Query("SELECT * FROM courses ORDER BY createdAtEpochMs DESC")
    fun getAllCourses(): Flow<List<Course>>

    @Query("""
        SELECT courses.*, COUNT(words.id) as wordCount 
        FROM courses 
        LEFT JOIN words ON courses.id = words.courseId 
        GROUP BY courses.id 
        ORDER BY createdAtEpochMs DESC
    """)
    fun getAllCoursesWithCount(): Flow<List<CourseWithCount>>

    @Insert
    suspend fun insertCourse(course: Course): Long

    // IELTS Passages
    @Query("SELECT * FROM ielts_passages ORDER BY createdAtEpochMs DESC")
    fun getAllIeltsPassages(): Flow<List<IeltsPassageEntity>>

    @Insert
    suspend fun insertIeltsPassage(passage: IeltsPassageEntity): Long

    @Query("SELECT * FROM ielts_passages WHERE id = :id")
    suspend fun getIeltsPassageRaw(id: Long): IeltsPassageEntity?

    @Update
    suspend fun updateIeltsPassage(passage: IeltsPassageEntity)

    @Delete
    suspend fun deleteIeltsPassage(passage: IeltsPassageEntity)

    // Daily Progress
    @Query("SELECT * FROM daily_progress WHERE date = :date")
    fun getDailyProgress(date: String): Flow<DailyProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyProgress(progress: DailyProgress)

    @Query("UPDATE daily_progress SET wordsLearned = wordsLearned + 1 WHERE date = :date")
    suspend fun incrementDailyProgress(date: String)

    // Words
    @Query("SELECT * FROM words WHERE courseId = :courseId")
    fun getWordsForCourse(courseId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE courseId IN (:courseIds)")
    suspend fun getWordsForCourseIds(courseIds: List<Long>): List<Word>

    @Insert
    suspend fun insertWords(words: List<Word>): List<Long>

    // Review & SRS
    @Update
    suspend fun updateReviewState(state: ReviewState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewState(state: ReviewState)

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): Course?

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM words WHERE courseId = :courseId")
    suspend fun deleteWordsForCourse(courseId: Long)

    @Query("SELECT * FROM words")
    suspend fun getAllWordsRaw(): List<Word>

    @Query("SELECT * FROM review_states")
    suspend fun getAllReviewStatesRaw(): List<ReviewState>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewStates(states: List<ReviewState>)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    @Query("DELETE FROM review_states")
    suspend fun deleteAllReviewStates()

    @Query("DELETE FROM review_states WHERE wordId IN (SELECT id FROM words WHERE courseId = :courseId)")
    suspend fun deleteReviewStatesForCourse(courseId: Long)

    @Query("SELECT * FROM courses WHERE name = :name LIMIT 1")
    suspend fun getCourseByName(name: String): Course?

    @Insert
    suspend fun insertWord(word: Word): Long

    @Update
    suspend fun updateWord(word: Word)

    @Delete
    suspend fun deleteWord(word: Word)

    @Delete
    suspend fun deleteWords(words: List<Word>)

    @Query("DELETE FROM review_states WHERE wordId IN (:wordIds)")
    suspend fun deleteReviewStatesForWords(wordIds: List<Long>)

    @Query("DELETE FROM review_states WHERE wordId = :wordId")
    suspend fun deleteReviewStateForWord(wordId: Long)

    @Query("SELECT * FROM review_states WHERE wordId = :wordId")
    suspend fun getReviewStateForWord(wordId: Long): ReviewState?

    @Query("""
        SELECT words.* FROM words 
        INNER JOIN courses ON words.courseId = courses.id 
        WHERE words.id NOT IN (:excludeIds) AND courses.nativeLanguageCode = :nativeLanguageCode 
        ORDER BY RANDOM() LIMIT :count
    """)
    suspend fun getRandomWordsByLanguage(excludeIds: List<Long>, nativeLanguageCode: String, count: Int): List<Word>

    @Query("""
        SELECT words.* FROM words 
        INNER JOIN review_states ON words.id = review_states.wordId 
        WHERE words.courseId = :courseId AND review_states.dueDateEpochMs <= :currentTime
    """)
    fun getDueWords(courseId: Long, currentTime: Long): Flow<List<Word>>

    @Query("""
        SELECT COUNT(*) FROM words 
        INNER JOIN review_states ON words.id = review_states.wordId 
        WHERE review_states.dueDateEpochMs <= :currentTime
    """)
    suspend fun getTotalDueWordsCount(currentTime: Long): Int
}
