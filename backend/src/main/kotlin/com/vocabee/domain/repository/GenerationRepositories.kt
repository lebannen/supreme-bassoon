package com.vocabee.domain.repository

import com.vocabee.domain.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourseGenerationRepository : JpaRepository<CourseGeneration, UUID> {
    fun findByCurrentStageNot(stage: GenerationStage): List<CourseGeneration>
    fun findByCourseId(courseId: Long): CourseGeneration?

    @Query("SELECT g FROM CourseGeneration g ORDER BY g.createdAt DESC")
    fun findAllOrderByCreatedAtDesc(): List<CourseGeneration>
}

@Repository
interface GenerationBlueprintRepository : JpaRepository<GenerationBlueprint, UUID> {
    fun findByGenerationId(generationId: UUID): GenerationBlueprint?
}

@Repository
interface GenerationCharacterRepository : JpaRepository<GenerationCharacter, UUID> {
    fun findByGenerationId(generationId: UUID): List<GenerationCharacter>
    fun findByGenerationIdAndName(generationId: UUID, name: String): GenerationCharacter?
}

@Repository
interface GenerationModulePlanRepository : JpaRepository<GenerationModulePlan, UUID> {
    fun findByGenerationId(generationId: UUID): List<GenerationModulePlan>
    fun findByGenerationIdOrderByModuleNumber(generationId: UUID): List<GenerationModulePlan>
    fun findByGenerationIdAndModuleNumber(generationId: UUID, moduleNumber: Int): GenerationModulePlan?
    fun countByGenerationIdAndStatus(generationId: UUID, status: GenerationStepStatus): Int
}

@Repository
interface GenerationEpisodePlanRepository : JpaRepository<GenerationEpisodePlan, UUID> {
    fun findByModulePlanId(modulePlanId: UUID): List<GenerationEpisodePlan>
    fun findByModulePlanIdOrderByEpisodeNumber(modulePlanId: UUID): List<GenerationEpisodePlan>
    fun findByModulePlanIdAndEpisodeNumber(modulePlanId: UUID, episodeNumber: Int): GenerationEpisodePlan?

    @Query(
        """
        SELECT ep FROM GenerationEpisodePlan ep
        JOIN GenerationModulePlan mp ON ep.modulePlanId = mp.id
        WHERE mp.generationId = :generationId
        ORDER BY mp.moduleNumber, ep.episodeNumber
    """
    )
    fun findAllByGenerationIdOrdered(generationId: UUID): List<GenerationEpisodePlan>

    @Query(
        """
        SELECT COUNT(ep) FROM GenerationEpisodePlan ep
        JOIN GenerationModulePlan mp ON ep.modulePlanId = mp.id
        WHERE mp.generationId = :generationId AND ep.status = :status
    """
    )
    fun countByGenerationIdAndStatus(generationId: UUID, status: GenerationStepStatus): Int
}

@Repository
interface GenerationEpisodeContentRepository : JpaRepository<GenerationEpisodeContent, UUID> {
    fun findByEpisodePlanId(episodePlanId: UUID): GenerationEpisodeContent?

    @Query(
        """
        SELECT ec FROM GenerationEpisodeContent ec
        JOIN GenerationEpisodePlan ep ON ec.episodePlanId = ep.id
        JOIN GenerationModulePlan mp ON ep.modulePlanId = mp.id
        WHERE mp.generationId = :generationId
        ORDER BY mp.moduleNumber, ep.episodeNumber
    """
    )
    fun findAllByGenerationIdOrdered(generationId: UUID): List<GenerationEpisodeContent>

    @Query(
        """
        SELECT ec FROM GenerationEpisodeContent ec
        JOIN GenerationEpisodePlan ep ON ec.episodePlanId = ep.id
        JOIN GenerationModulePlan mp ON ep.modulePlanId = mp.id
        WHERE mp.generationId = :generationId
        AND (mp.moduleNumber < :moduleNumber
             OR (mp.moduleNumber = :moduleNumber AND ep.episodeNumber < :episodeNumber))
        ORDER BY mp.moduleNumber, ep.episodeNumber
    """
    )
    fun findPreviousEpisodes(
        generationId: UUID,
        moduleNumber: Int,
        episodeNumber: Int
    ): List<GenerationEpisodeContent>

    @Query(
        """
        SELECT COUNT(ec) FROM GenerationEpisodeContent ec
        JOIN GenerationEpisodePlan ep ON ec.episodePlanId = ep.id
        JOIN GenerationModulePlan mp ON ep.modulePlanId = mp.id
        WHERE mp.generationId = :generationId AND ec.status = :status
    """
    )
    fun countByGenerationIdAndStatus(generationId: UUID, status: GenerationStepStatus): Int
}

@Repository
interface GenerationExercisesRepository : JpaRepository<GenerationExercises, UUID> {
    fun findByEpisodePlanId(episodePlanId: UUID): GenerationExercises?

    @Query(
        """
        SELECT ex FROM GenerationExercises ex
        JOIN GenerationEpisodePlan ep ON ex.episodePlanId = ep.id
        JOIN GenerationModulePlan mp ON ep.modulePlanId = mp.id
        WHERE mp.generationId = :generationId
        ORDER BY mp.moduleNumber, ep.episodeNumber
    """
    )
    fun findAllByGenerationIdOrdered(generationId: UUID): List<GenerationExercises>

    @Query(
        """
        SELECT COUNT(ex) FROM GenerationExercises ex
        JOIN GenerationEpisodePlan ep ON ex.episodePlanId = ep.id
        JOIN GenerationModulePlan mp ON ep.modulePlanId = mp.id
        WHERE mp.generationId = :generationId AND ex.status = :status
    """
    )
    fun countByGenerationIdAndStatus(generationId: UUID, status: GenerationStepStatus): Int
}

@Repository
interface GenerationMediaRepository : JpaRepository<GenerationMedia, UUID> {
    fun findByGenerationId(generationId: UUID): List<GenerationMedia>
    fun findByEpisodePlanId(episodePlanId: UUID): List<GenerationMedia>
    fun findByCharacterId(characterId: UUID): List<GenerationMedia>
    fun findByGenerationIdAndMediaType(generationId: UUID, mediaType: String): List<GenerationMedia>
    fun countByGenerationIdAndStatus(generationId: UUID, status: GenerationStepStatus): Int
    fun deleteByGenerationIdAndMediaType(generationId: UUID, mediaType: String)
}
