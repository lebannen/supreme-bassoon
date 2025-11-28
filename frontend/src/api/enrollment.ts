import {BaseAPI} from './base'

export interface EnrollmentResponse {
    id: number
    courseId: number
    courseSlug: string
    courseName: string
    languageCode: string
    cefrLevel: string
    currentModuleId: number | null
    currentModuleName: string | null
    currentEpisodeId: number | null
    currentEpisodeName: string | null
    completedEpisodes: number
    totalEpisodes: number
    progress: number
    enrolledAt: string
    lastActivityAt: string
}

class EnrollmentAPI extends BaseAPI {
    async getMyEnrollments(): Promise<EnrollmentResponse[]> {
        return this.get<EnrollmentResponse[]>('/api/enrollments')
    }

    async getEnrollment(courseId: number): Promise<EnrollmentResponse | null> {
        try {
            return await this.get<EnrollmentResponse>(`/api/enrollments/course/${courseId}`)
        } catch {
            return null
        }
    }

    async checkEnrollment(courseId: number): Promise<boolean> {
        try {
            const response = await this.get<{ enrolled: boolean }>(`/api/enrollments/course/${courseId}/check`)
            return response.enrolled
        } catch {
            return false
        }
    }

    async enrollInCourse(courseId: number): Promise<EnrollmentResponse> {
        return this.post<EnrollmentResponse>(`/api/enrollments/course/${courseId}`)
    }

    async updatePosition(courseId: number, moduleId: number, episodeId: number): Promise<void> {
        await this.post(`/api/enrollments/course/${courseId}/position`, {
            moduleId,
            episodeId
        })
    }

    async completeEpisode(episodeId: number): Promise<{ episodeId: number; isCompleted: boolean }> {
        return this.post(`/api/enrollments/episodes/${episodeId}/complete`)
    }
}

export const enrollmentAPI = new EnrollmentAPI()
