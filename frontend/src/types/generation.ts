export interface GenerateModuleRequest {
    theme: string
    level: string
    targetLanguage: string
    contentType: string
    focus?: string
    additionalInstructions?: string
}

export interface GenerateStructureRequest {
    script: string
    theme: string
    level: string
    targetLanguage: string
}

export interface GenerateExerciseRequest {
    context: string
    type: string
    targetLanguage: string
    level: string
    instructions?: string
}

export interface GeneratedModule {
    title: string
    theme: string
    description: string
    objectives: string[]
    episodes: GeneratedEpisode[]
}

export interface GeneratedEpisode {
    title: string
    type: 'DIALOGUE' | 'STORY'
    dialogue?: GeneratedDialogue
    contentItems: GeneratedContentItem[]
}

export interface GeneratedDialogue {
    lines: GeneratedDialogueLine[]
    speakers: Record<string, GeneratedSpeaker>
}

export interface GeneratedDialogueLine {
    speaker: string
    text: string
}

export interface GeneratedSpeaker {
    voice: string
    description: string
}

export interface GeneratedContentItem {
    type: string
    exercise: GeneratedExercise
}

export interface GeneratedExercise {
    type: string
    meta?: Record<string, any>
    content: Record<string, any>
}

export interface ModuleGenerationResponse {
    module: GeneratedModule
    validationErrors: string[]
}
