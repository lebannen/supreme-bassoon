import {BaseAPI} from '@/api/base'
import type {
    StartGenerationRequest,
    RegenerateFeedback,
    GenerationProgressResponse,
    GenerationSummaryDto,
    PublishedCourseDto,
    GenerationDebugResponse
} from '@/types/pipeline'

const BASE_URL = '/api/admin/generations'

class PipelineAPI extends BaseAPI {
    /**
     * Start a new course generation workflow
     */
    async startGeneration(request: StartGenerationRequest): Promise<GenerationProgressResponse> {
        return this.post<GenerationProgressResponse>(BASE_URL, request)
    }

    /**
     * Get current progress of a generation workflow
     */
    async getProgress(generationId: string): Promise<GenerationProgressResponse> {
        return this.get<GenerationProgressResponse>(`${BASE_URL}/${generationId}`)
    }

    /**
     * List all generation workflows
     */
    async listGenerations(): Promise<GenerationSummaryDto[]> {
        return this.get<GenerationSummaryDto[]>(BASE_URL)
    }

    /**
     * Approve current stage and proceed to the next one
     */
    async proceedToNextStage(generationId: string): Promise<GenerationProgressResponse> {
        return this.post<GenerationProgressResponse>(`${BASE_URL}/${generationId}/proceed`)
    }

    /**
     * Regenerate the current stage with optional feedback
     */
    async regenerateCurrentStage(
        generationId: string,
        feedback?: RegenerateFeedback
    ): Promise<GenerationProgressResponse> {
        return this.post<GenerationProgressResponse>(
            `${BASE_URL}/${generationId}/regenerate`,
            feedback
        )
    }

    /**
     * Cancel and delete a generation workflow
     */
    async cancelGeneration(generationId: string): Promise<void> {
        return this.delete(`${BASE_URL}/${generationId}`)
    }

    /**
     * Publish a completed generation to an actual course
     */
    async publishCourse(generationId: string): Promise<PublishedCourseDto> {
        return this.post<PublishedCourseDto>(`${BASE_URL}/${generationId}/publish`)
    }

    /**
     * Get debug data for a generation including all raw AI responses
     */
    async getDebugData(generationId: string): Promise<GenerationDebugResponse> {
        return this.get<GenerationDebugResponse>(`${BASE_URL}/${generationId}/debug`)
    }
}

export const PipelineService = new PipelineAPI()
export default PipelineService
