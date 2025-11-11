package com.vocabee.domain.repository

import com.vocabee.domain.model.ExerciseType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExerciseTypeRepository : JpaRepository<ExerciseType, Int> {
    fun findByTypeKey(typeKey: String): ExerciseType?
}
