package com.vocabee.domain.repository

import com.vocabee.domain.model.Module
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ModuleRepository : JpaRepository<Module, Long> {
    fun findByCourseIdOrderByModuleNumber(courseId: Long): List<Module>
    fun findByCourseIdAndModuleNumber(courseId: Long, moduleNumber: Int): Module?
}
