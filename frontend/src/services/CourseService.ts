import {BaseAPI} from '@/api/base'
import type {GeneratedEpisodeSummary, GeneratedSyllabus} from '@/types/generation'

export interface CreateCourseRequest {
    name: string
    targetLanguage: string
    level: string
    seriesContext: string
    syllabus: GeneratedSyllabus
    enrichedModules?: any[]
}

export interface CourseAdminDto {
    id: number
    slug: string
    name: string
    languageCode: string
    cefrLevel: string
    estimatedHours: number
    isPublished: boolean
    seriesContext?: string
    createdAt?: string
}

export interface ModuleAdminDto {
    id: number
    moduleNumber: number
    title: string
    episodeCount: number
}

export interface ModuleDetailDto {
    id: number
    moduleNumber: number
    title: string
    theme?: string
    description?: string
    objectives?: string[]
    vocabularyFocus?: string[]
    grammarFocus?: string[]
    episodeOutline?: GeneratedEpisodeSummary[]
    episodeCount: number
}

class CourseAPI extends BaseAPI {
    async getAllCourses(): Promise<CourseAdminDto[]> {
        return this.get<CourseAdminDto[]>('/api/admin/courses')
    }

    async getCourse(courseId: number): Promise<CourseAdminDto> {
        return this.get<CourseAdminDto>(`/api/admin/courses/${courseId}`)
    }

    async createCourse(request: CreateCourseRequest): Promise<CourseAdminDto> {
        return this.post<CourseAdminDto>('/api/admin/courses', request)
    }

    async getModulesForCourse(courseId: number): Promise<ModuleAdminDto[]> {
        return this.get<ModuleAdminDto[]>(`/api/admin/courses/${courseId}/modules`)
    }

    async getModule(moduleId: number): Promise<ModuleDetailDto> {
        return this.get<ModuleDetailDto>(`/api/admin/courses/modules/${moduleId}`)
    }

    async saveModule(moduleId: number, request: any): Promise<ModuleAdminDto> {
        return this.put<ModuleAdminDto>(`/api/admin/courses/modules/${moduleId}`, request)
    }

    async deleteModule(moduleId: number): Promise<void> {
        return this.delete<void>(`/api/admin/courses/modules/${moduleId}`)
    }

    async generateAllCourseContent(courseId: number): Promise<any> {
        return this.post<any>(`/api/admin/courses/${courseId}/generate-all`)
    }

    async saveCourseContent(request: any): Promise<any> {
        return this.post<any>('/api/admin/courses/save-course-content', request)
    }

    async getLatestReview(courseId: number): Promise<CourseReviewDto | null> {
        try {
            return await this.get<CourseReviewDto>(`/api/admin/courses/${courseId}/reviews/latest`)
        } catch (e: any) {
            // 404 means no review exists yet - return null
            if (e.message === 'Resource not found') {
                return null
            }
            throw e
        }
    }

    async createReview(courseId: number): Promise<CourseReviewDto> {
        return this.post<CourseReviewDto>(`/api/admin/courses/${courseId}/review`)
    }
}

export interface CourseReviewDto {
    id: number
    overallScore: number | null
    cefrAlignmentScore: number | null
    structureScore: number | null
    contentQualityScore: number | null
    summary: string
    strengths: string[]
    weaknesses: string[]
    recommendations: string[]
    moduleFeedback: { moduleNumber: number; score: number | null; feedback: string }[]
    detailedAnalysis: string | null
    createdAt: string
}

export const CourseService = new CourseAPI()
