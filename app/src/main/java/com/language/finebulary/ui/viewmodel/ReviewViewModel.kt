package com.language.finebulary.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.language.finebulary.data.model.ReviewState
import com.language.finebulary.data.model.Word
import com.language.finebulary.data.model.commonDistractors
import com.language.finebulary.data.pref.UserPreferences
import com.language.finebulary.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.min

enum class QuestionType {
    MULTIPLE_CHOICE, SPELLING, PRONUNCIATION, WRITING, MATCHING
}

enum class StudyMode {
    WORD, SENTENCE
}

enum class ReviewStatus {
    CORRECT, CLOSE, WRONG
}

data class ReviewResult(
    val word: Word,
    val status: ReviewStatus,
    val reviewState: ReviewState? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val userPreferences: UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val courseId: Long = checkNotNull(savedStateHandle["courseId"])
    private val studyModeArg: String = savedStateHandle["studyMode"] ?: StudyMode.WORD.name
    private val enabledMethods: String = savedStateHandle["methods"] ?: "MULTIPLE_CHOICE,SPELLING"

    // Logic Preferences
    private var masteryTargetValue = 3
    val soundEnabled = userPreferences.soundEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hapticEnabled = userPreferences.hapticEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord

    private val _questionType = MutableStateFlow(QuestionType.MULTIPLE_CHOICE)
    val questionType: StateFlow<QuestionType> = _questionType

    private val _studyMode = MutableStateFlow(StudyMode.valueOf(studyModeArg))
    val studyMode: StateFlow<StudyMode> = _studyMode

    private val _options = MutableStateFlow<List<String>>(emptyList())
    val options: StateFlow<List<String>> = _options

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private val _sessionProgress = MutableStateFlow(0f)
    val sessionProgress: StateFlow<Float> = _sessionProgress

    private val _questionKey = MutableStateFlow(0)
    val questionKey: StateFlow<Int> = _questionKey

    private val _reviewResult = MutableStateFlow<ReviewResult?>(null)
    val reviewResult: StateFlow<ReviewResult?> = _reviewResult

    private val _matchingWords = MutableStateFlow<List<Word>>(emptyList())
    val matchingWords: StateFlow<List<Word>> = _matchingWords

    private val _matchingMeanings = MutableStateFlow<List<String>>(emptyList())
    val matchingMeanings: StateFlow<List<String>> = _matchingMeanings

    private val _selectedLeftWord = MutableStateFlow<Word?>(null)
    val selectedLeftWord: StateFlow<Word?> = _selectedLeftWord

    private val _selectedRightMeaning = MutableStateFlow<String?>(null)
    val selectedRightMeaning: StateFlow<String?> = _selectedRightMeaning

    private val _matchedWordIds = MutableStateFlow<Set<Long>>(emptySet())
    val matchedWordIds: StateFlow<Set<Long>> = _matchedWordIds

    val todayProgress: StateFlow<Int> = flow {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        emitAll(repository.getDailyProgress(today).map { it?.wordsLearned ?: 0 })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dailyGoal = userPreferences.dailyGoal.stateIn(viewModelScope, SharingStarted.Eagerly, 10)

    private var sessionWords = mutableListOf<Word>()
    private var nativeLanguageCode: String = "en"
    private val masteryScores = mutableMapOf<Long, Int>()
    private val failedWords = mutableSetOf<Long>()
    
    private var activeMethodsList = listOf<QuestionType>()

    init {
        viewModelScope.launch {
            userPreferences.masteryTarget.collect { masteryTargetValue = it }
        }
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val course = repository.getCourseById(courseId)
            nativeLanguageCode = course?.nativeLanguageCode ?: "en"

            var methods = (enabledMethods.split(",") + "MATCHING").map { QuestionType.valueOf(it.trim()) }.distinct()
            if (course?.cantTalkRightNow == true) {
                methods = methods.filter { it != QuestionType.PRONUNCIATION }
            }
            activeMethodsList = if (methods.isEmpty()) listOf(QuestionType.MULTIPLE_CHOICE) else methods

            val dueWords = repository.getDueWords(courseId).first().shuffled().take(10)
            
            if (dueWords.isEmpty()) {
                _isFinished.value = true
                return@launch
            }

            sessionWords = dueWords.toMutableList()
            dueWords.forEach { masteryScores[it.id] = 0 }
            
            nextQuestion()
        }
    }

    private fun nextQuestion() {
        val remainingWords = sessionWords.filter { (masteryScores[it.id] ?: 0) < masteryTargetValue }
        
        if (remainingWords.isEmpty()) {
            _currentWord.value = null
            _isFinished.value = true
            _sessionProgress.value = 1f
            return
        }

        val nextWord = remainingWords.random()
        _currentWord.value = nextWord
        
        val currentScore = masteryScores[nextWord.id] ?: 0
        _questionType.value = pickMethodForScore(currentScore)
        _questionKey.value += 1

        if (_questionType.value == QuestionType.MULTIPLE_CHOICE) {
            generateOptions(nextWord)
        } else if (_questionType.value == QuestionType.MATCHING) {
            initMatchingSession()
        }
        
        updateProgress()
    }

    private fun pickMethodForScore(score: Int): QuestionType {
        if (activeMethodsList.size == 1) return activeMethodsList[0]
        return when (score) {
            0 -> activeMethodsList.find { it == QuestionType.MULTIPLE_CHOICE } ?: activeMethodsList[0]
            1 -> activeMethodsList.find { it != QuestionType.MULTIPLE_CHOICE } ?: activeMethodsList[0]
            else -> activeMethodsList.last()
        }
    }

    private fun generateOptions(word: Word) {
        viewModelScope.launch {
            val correctTranslation = word.backsPacked.split("|").first().trim()
            val dbDistractors = repository.getRandomDistractors(
                excludeIds = listOf(word.id), 
                nativeLanguageCode = nativeLanguageCode,
                count = 10
            )
            val languageFallbacks = commonDistractors[nativeLanguageCode] ?: emptyList()
            val allDistractors = (dbDistractors + languageFallbacks.shuffled().take(10))
                .filter { it.lowercase() != correctTranslation.lowercase() }
                .distinct()
                .shuffled()
                .take(3)
            
            _options.value = (allDistractors + correctTranslation).shuffled()
        }
    }

    private fun updateProgress() {
        val totalPointsEarned = masteryScores.values.sum()
        val totalPointsNeeded = sessionWords.size * masteryTargetValue
        _sessionProgress.value = totalPointsEarned.toFloat() / totalPointsNeeded.toFloat()
    }

    fun submitAnswer(answer: String) {
        val word = _currentWord.value ?: return
        val targetText = if (_questionType.value == QuestionType.MULTIPLE_CHOICE || _questionType.value == QuestionType.SPELLING) {
            word.backsPacked.split("|").first().trim()
        } else {
            word.front
        }

        val guess = answer.trim().lowercase()
        val target = targetText.lowercase()

        val status = when {
            guess == target -> ReviewStatus.CORRECT
            isClose(guess, target) -> ReviewStatus.CLOSE
            else -> ReviewStatus.WRONG
        }
        
        viewModelScope.launch {
            if (status == ReviewStatus.CORRECT) {
                masteryScores[word.id] = (masteryScores[word.id] ?: 0) + 1
                if ((masteryScores[word.id] ?: 0) >= masteryTargetValue) {
                    val rating = if (failedWords.contains(word.id)) "HARD" else "GOOD"
                    repository.updateReview(courseId, word.id, rating)
                    
                    // Increment daily progress when a word is fully mastered/reviewed in session
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    repository.incrementDailyProgress(today)
                }
            } else if (status == ReviewStatus.WRONG) {
                masteryScores[word.id] = maxOf(0, (masteryScores[word.id] ?: 0) - 1)
                failedWords.add(word.id)
            }

            val state = repository.getReviewState(word.id)
            _reviewResult.value = ReviewResult(word, status, state)
        }
    }

    private fun isClose(guess: String, target: String): Boolean {
        if (target.length < 3) return false
        val distance = levenshtein(guess, target)
        return when {
            target.length <= 5 -> distance == 1
            else -> distance <= 2
        }
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun initMatchingSession() {
        val words = sessionWords.filter { (masteryScores[it.id] ?: 0) < masteryTargetValue }.shuffled().take(5)
        _matchingWords.value = words
        _matchingMeanings.value = words.map { it.backsPacked.split("|").first().trim() }.shuffled()
        _matchedWordIds.value = emptySet()
        _selectedLeftWord.value = null
        _selectedRightMeaning.value = null
    }

    fun selectLeftWord(word: Word) {
        if (_matchedWordIds.value.contains(word.id)) return
        _selectedLeftWord.value = word
        checkMatch()
    }

    fun selectRightMeaning(meaning: String) {
        val left = _selectedLeftWord.value ?: return
        _selectedRightMeaning.value = meaning
        checkMatch()
    }

    private fun checkMatch() {
        val left = _selectedLeftWord.value
        val right = _selectedRightMeaning.value
        if (left != null && right != null) {
            val correctMeaning = left.backsPacked.split("|").first().trim()
            if (right.equals(correctMeaning, ignoreCase = true)) {
                _matchedWordIds.value = _matchedWordIds.value + left.id
                masteryScores[left.id] = (masteryScores[left.id] ?: 0) + 1
                if ((masteryScores[left.id] ?: 0) >= masteryTargetValue) {
                    viewModelScope.launch {
                        repository.updateReview(courseId, left.id, "GOOD")
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        repository.incrementDailyProgress(today)
                    }
                }
                _selectedLeftWord.value = null
                _selectedRightMeaning.value = null

                if (_matchedWordIds.value.size >= _matchingWords.value.size) {
                    viewModelScope.launch {
                        delay(500)
                        nextQuestion()
                    }
                }
            } else {
                _selectedLeftWord.value = null
                _selectedRightMeaning.value = null
            }
        }
    }

    fun passCurrentWord() {
        val word = _currentWord.value ?: return
        failedWords.add(word.id)
        masteryScores[word.id] = maxOf(0, (masteryScores[word.id] ?: 0) - 1)
        nextQuestion()
    }

    fun setCantTalkRightNow() {
        viewModelScope.launch {
            val course = repository.getCourseById(courseId)
            if (course != null) {
                repository.updateCourse(course.copy(cantTalkRightNow = true))
            }
            activeMethodsList = activeMethodsList.filter { it != QuestionType.PRONUNCIATION }
            if (activeMethodsList.isEmpty()) {
                activeMethodsList = listOf(QuestionType.MULTIPLE_CHOICE)
            }
            passCurrentWord()
        }
    }

    fun retryQuestion() {
        _reviewResult.value = null
        _questionKey.value += 1
    }

    fun dismissResult() {
        _reviewResult.value = null
        nextQuestion()
    }

    fun restartSession() {
        viewModelScope.launch {
            repository.restartCourse(courseId)
            _isFinished.value = false
            masteryScores.clear()
            failedWords.clear()
            loadSession()
        }
    }
}
