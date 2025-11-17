import {BaseAPI} from './base'
import type {CourseDetail, CourseSummary, Episode, Module, ModuleProgress,} from '@/types/course'

/**
 * API client for courses, modules, and episodes
 */
export class CourseAPI extends BaseAPI {
    // ============ Course endpoints ============

    /**
     * Get all courses
     */
    async getCourses(): Promise<CourseSummary[]> {
        return this.get<CourseSummary[]>('/api/courses')
    }

    /**
     * Get a specific course by slug
     */
    async getCourseBySlug(slug: string): Promise<CourseDetail> {
        return this.get<CourseDetail>(`/api/courses/${slug}`)
    }

    /**
     * Get a specific course by ID
     */
    async getCourseById(id: number): Promise<CourseDetail> {
        return this.get<CourseDetail>(`/api/courses/${id}`)
    }

    // ============ Module endpoints ============

    /**
     * Get a specific module by ID
     */
    async getModule(moduleId: number): Promise<Module> {
        return this.get<Module>(`/api/modules/${moduleId}`)
    }

    /**
     * Get module progress for a user
     */
    async getModuleProgress(
        languageCode: string,
        moduleNumber: number
    ): Promise<ModuleProgress> {
        return this.get<ModuleProgress>(
            `/api/exercises/progress/module/${languageCode}/${moduleNumber}`
        )
    }

    // ============ Episode endpoints ============

    /**
     * Get a specific episode by ID
     */
    async getEpisode(episodeId: number): Promise<Episode> {
        return this.get<Episode>(`/api/episodes/${episodeId}`)
    }

    /**
     * Mark an episode as complete
     */
    async completeEpisode(episodeId: number): Promise<void> {
        return this.post<void>(`/api/episodes/${episodeId}/complete`)
    }
}
