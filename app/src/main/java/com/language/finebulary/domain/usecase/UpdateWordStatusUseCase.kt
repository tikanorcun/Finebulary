package com.language.finebulary.domain.usecase

import com.language.finebulary.data.model.Word
import com.language.finebulary.repository.CourseRepository
import javax.inject.Inject

class UpdateWordStatusUseCase @Inject constructor(
    private val repository: CourseRepository
) {
    suspend operator fun invoke(word: Word) {
        repository.updateWord(word)
    }
}
