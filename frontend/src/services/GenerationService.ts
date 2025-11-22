import { BaseAPI } from '@/api/base'
import type { GenerateModuleRequest, GenerateStructureRequest, GenerateExerciseRequest, ModuleGenerationResponse, GeneratedModule, GeneratedContentItem } from '@/types/generation'

class GenerationAPI extends BaseAPI {
    async generateModule(request: GenerateModuleRequest): Promise<ModuleGenerationResponse> {
        return this.post<ModuleGenerationResponse>('/api/admin/generation/module', request)
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

    async validateModule(module: GeneratedModule): Promise<string[]> {
        return this.post<string[]>('/api/admin/generation/validate', module)
    }
}

export const GenerationService = new GenerationAPI()
