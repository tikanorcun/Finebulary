package com.language.finebulary.data.model

import androidx.room.Embedded

data class CourseWithCount(
    @Embedded val course: Course,
    val wordCount: Int
)
