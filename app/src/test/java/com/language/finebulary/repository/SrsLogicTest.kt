package com.language.finebulary.repository

import com.language.finebulary.data.model.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Isolated test for the SRS algorithm logic.
 * Ensures the math behind 'Easy', 'Good', and 'Hard' intervals is correct.
 */
class SrsLogicTest {

    @Test
    fun `test 'Easy' rating first time results in 14 day interval`() {
        val initialState = ReviewState(wordId = 1)
        val rating = "EASY"
        
        // Simulating the logic inside CourseRepository.updateReview
        var newEase = initialState.ease
        var newInterval = initialState.intervalDays

        when (rating) {
            "EASY" -> {
                newInterval = if (newInterval == 0) 14 else max(14, (newInterval * 2.0).toInt())
                newEase = min(2.8, newEase + 0.15)
            }
        }

        assertEquals(14, newInterval)
        assertEquals(2.65, newEase, 0.01)
    }

    @Test
    fun `test 'Hard' rating decreases ease and sets 1 day interval`() {
        val initialState = ReviewState(wordId = 1, ease = 2.5, intervalDays = 7)
        val rating = "HARD"
        
        var newEase = initialState.ease
        var newInterval = initialState.intervalDays

        when (rating) {
            "HARD" -> {
                newInterval = 1
                newEase = max(1.3, newEase - 0.2)
            }
        }

        assertEquals(1, newInterval)
        assertEquals(2.3, newEase, 0.01)
    }

    @Test
    fun `test 'Good' rating increases interval by 30 percent`() {
        val initialState = ReviewState(wordId = 1, intervalDays = 10)
        val rating = "GOOD"
        
        var newInterval = initialState.intervalDays

        when (rating) {
            "GOOD" -> {
                newInterval = if (newInterval == 0) 7 else max(7, (newInterval * 1.3).toInt())
            }
        }

        assertEquals(13, newInterval)
    }
}
