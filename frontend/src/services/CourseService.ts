import { BaseAPI } from '@/api/base'
import type { GeneratedSyllabus } from '@/types/generation'

export interface CreateCourseRequest {
    targetLanguage: string
    level: string
    seriesContext: string
    syllabus: GeneratedSyllabus
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
}

export interface ModuleAdminDto {
    id: number
    moduleNumber: number
    title: string
    episodeCount: number
}

class CourseAPI extends BaseAPI {
    async getAllCourses(): Promise<CourseAdminDto[]> {
        return this.get<CourseAdminDto[]>('/api/admin/courses')
    }

    async createCourse(request: CreateCourseRequest): Promise<CourseAdminDto> {
        return this.post<CourseAdminDto>('/api/admin/courses', request)
    }

    async getModulesForCourse(courseId: number): Promise<ModuleAdminDto[]> {
        return this.get<ModuleAdminDto[]>(`/api/admin/courses/${courseId}/modules`)
    }

    async saveModule(moduleId: number, request: any): Promise<ModuleAdminDto> {
        return this.put<ModuleAdminDto>(`/api/admin/courses/modules/${moduleId}`, request)
    }

    async deleteModule(moduleId: number): Promise<void> {
        return this.delete<void>(`/api/admin/courses/modules/${moduleId}`)
    }
}

export const CourseService = new CourseAPI()
