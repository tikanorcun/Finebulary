package com.language.finebulary.domain.usecase

import com.language.finebulary.data.model.Course
import com.language.finebulary.repository.CourseRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRecommendedCourseUseCase @Inject constructor(
    private val repository: CourseRepository
) {
    suspend operator fun invoke(): Course? {
        val courses = repository.allCourses.first()
        // Eğer sadece 1 kurs varsa onu öner
        return if (courses.size == 1) {
            courses.first()
        } else {
            // İleride buraya son çalışılan kurs mantığı eklenebilir
            courses.maxByOrNull { it.currentStreak }
        }
    }
}
