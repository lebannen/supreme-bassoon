
import { BaseAPI } from '@/api/base'
import type {
    GenerateBatchExercisesRequest,
    GenerateExerciseRequest,
    GenerateModuleRequest,
    GenerateStructureRequest,
    GenerateSyllabusRequest,
    GeneratedContentItem,
    GeneratedModule,
    GeneratedSyllabus,
    GenerateOutlineRequest,
    GeneratedOutline,
    GenerateEpisodeContentRequest,
    GeneratedEpisodeContent,
    ModuleGenerationResponse
} from '@/types/generation'

class GenerationAPI extends BaseAPI {
    async generateModule(request: GenerateModuleRequest): Promise<ModuleGenerationResponse> {
        return this.post<ModuleGenerationResponse>('/api/admin/generation/generate', request)
    }

    async generateDialogue(request: GenerateModuleRequest): Promise<string> {
        const response = await this.post<{ script: string }>('/api/admin/generation/dialogue', request)
        return response.script
    }

    async generateStructure(request: GenerateStructureRequest): Promise<ModuleGenerationResponse> {
        return this.post<ModuleGenerationResponse>('/api/admin/generation/structure', request)
    }

    async generateExercise(request: GenerateExerciseRequest): Promise<GeneratedContentItem> {
        return this.post<GeneratedContentItem>('/api/admin/generation/exercise', request)
    }

    async generateBatchExercises(request: GenerateBatchExercisesRequest): Promise<GeneratedContentItem[]> {
        return this.post<GeneratedContentItem[]>('/api/admin/generation/exercises/batch', request)
    }

    async generateSyllabus(request: GenerateSyllabusRequest): Promise<GeneratedSyllabus> {
        return this.post<GeneratedSyllabus>('/api/admin/generation/syllabus', request)
    }

    async generateOutline(request: GenerateOutlineRequest): Promise<GeneratedOutline> {
        return this.post<GeneratedOutline>('/api/admin/generation/outline', request)
    }

    async generateEpisodeContent(request: GenerateEpisodeContentRequest): Promise<GeneratedEpisodeContent> {
        return this.post<GeneratedEpisodeContent>('/api/admin/generation/episode', request)
    }

    async validateModule(module: GeneratedModule): Promise<string[]> {
        return this.post<string[]>('/api/admin/generation/validate', module)
    }
}

export const GenerationService = new GenerationAPI()
