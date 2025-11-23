package com.vocabee.web.dto.admin

import com.vocabee.domain.generation.GeneratedSyllabus

data class CreateCourseRequest(
    val targetLanguage: String,
    val level: String,
    val seriesContext: String,
    val syllabus: GeneratedSyllabus
)
