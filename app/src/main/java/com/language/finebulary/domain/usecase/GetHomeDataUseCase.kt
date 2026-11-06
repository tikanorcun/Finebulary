package com.language.finebulary.domain.usecase

import com.language.finebulary.repository.CourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetHomeDataUseCase @Inject constructor(
    private val repository: CourseRepository
) {
    operator fun invoke(): Flow<Int> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return repository.getDailyProgress(today).map { it?.wordsLearned ?: 0 }
    }
}
