/**
 * Types for the Course Generation Pipeline
 */

export type GenerationStage =
    | 'BLUEPRINT'
    | 'MODULE_PLANNING'
    | 'EPISODE_CONTENT'
    | 'VOCABULARY_LINKING'
    | 'CHARACTER_PROFILES'
    | 'EXERCISES'
    | 'MEDIA'
    | 'COMPLETED'
    | 'FAILED'

export type StepStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'

export interface StartGenerationRequest {
    languageCode: string
    cefrLevel: string
    moduleCount: number
    episodesPerModule: number
    themePreferences?: string
    autoMode?: boolean
}

export interface RegenerateFeedback {
    feedback?: string
}

export interface StageProgress {
    completed: number
    total: number
    currentItem?: string
}

export interface PlotArcPoint {
    moduleNumber: number
    plotPoint: string
}

export interface ModuleTopic {
    moduleNumber: number
    topic: string
    theme: string
}

export interface GrammarDistribution {
    moduleNumber: number
    grammarRules: string[]
}

export interface BlueprintDto {
    id: string
    courseTitle?: string
    courseDescription?: string
    setting?: string
    premise?: string
    plotArc?: PlotArcPoint[]
    moduleTopics?: ModuleTopic[]
    grammarDistribution?: GrammarDistribution[]
    status: StepStatus
}

export interface CharacterDto {
    id: string
    name: string
    role: string
    initialDescription?: string
    ageRange?: string
    personalityTraits?: string[]
    background?: string
    appearanceDescription?: string
    voiceId?: string
    referenceImageUrl?: string
}

export interface EpisodeContentDto {
    id: string
    contentType: string
    content?: string
    summary?: string
    vocabularyUsed?: string[]
    vocabularyMissing?: string[]
    status: StepStatus
}

export interface EpisodeExercisesDto {
    id: string
    exerciseCount: number
    vocabularyCoverage?: string[]
    grammarCoverage?: string[]
    status: StepStatus
}

export interface EpisodePlanDto {
    id: string
    episodeNumber: number
    title?: string
    sceneDescription?: string
    episodeType: string
    vocabulary?: string[]
    grammarRules?: string[]
    characterNames?: string[]
    plotPoints?: string
    content?: EpisodeContentDto
    exercises?: EpisodeExercisesDto
    status: StepStatus
}

export interface ModulePlanDto {
    id: string
    moduleNumber: number
    title?: string
    theme?: string
    description?: string
    objectives?: string[]
    plotSummary?: string
    episodes: EpisodePlanDto[]
    status: StepStatus
}

export interface GenerationProgressResponse {
    id: string
    languageCode: string
    cefrLevel: string
    moduleCount: number
    episodesPerModule: number
    themePreferences?: string
    currentStage: GenerationStage
    stageProgress: StageProgress
    blueprint?: BlueprintDto
    modulePlans?: ModulePlanDto[]
    characters?: CharacterDto[]
    canProceed: boolean
    canPublish: boolean
    errorMessage?: string
    createdAt: string
    completedAt?: string
}

export interface GenerationSummaryDto {
    id: string
    languageCode: string
    cefrLevel: string
    moduleCount: number
    currentStage: GenerationStage
    blueprintTitle?: string
    createdAt: string
    completedAt?: string
}

export interface PublishedCourseDto {
    id: number
    slug: string
    name: string
    languageCode: string
    cefrLevel: string
    description?: string
    objectives: string[]
    estimatedHours: number
    modules: unknown[]
}

// ============================================================================
// Debug/Raw Data Types
// ============================================================================

export interface GenerationDebugResponse {
    id: string
    languageCode: string
    cefrLevel: string
    moduleCount: number
    episodesPerModule: number
    themePreferences?: string
    currentStage: GenerationStage
    autoMode: boolean
    errorMessage?: string
    createdAt: string
    completedAt?: string
    blueprint?: BlueprintDebugDto
    modulePlans: ModulePlanDebugDto[]
    characters: CharacterDebugDto[]
    media: MediaDebugDto[]
}

export interface BlueprintDebugDto {
    id: string
    courseTitle?: string
    courseDescription?: string
    setting?: string
    premise?: string
    plotArc?: unknown
    moduleTopics?: unknown
    grammarDistribution?: unknown
    status: StepStatus
    rawResponse?: string
    createdAt?: string
}

export interface ModulePlanDebugDto {
    id: string
    moduleNumber: number
    title?: string
    theme?: string
    description?: string
    objectives?: unknown
    plotSummary?: string
    status: StepStatus
    rawResponse?: string
    createdAt?: string
    episodes: EpisodePlanDebugDto[]
}

export interface EpisodePlanDebugDto {
    id: string
    episodeNumber: number
    title?: string
    sceneDescription?: string
    episodeType: string
    vocabulary?: unknown
    grammarRules?: unknown
    characterIds?: unknown
    plotPoints?: string
    status: StepStatus
    createdAt?: string
    content?: EpisodeContentDebugDto
    exercises?: EpisodeExercisesDebugDto
}

export interface EpisodeContentDebugDto {
    id: string
    contentType?: string
    content?: string
    contentStructured?: unknown
    summary?: string
    characterDevelopments?: unknown
    vocabularyUsed?: unknown
    vocabularyMissing?: unknown
    imagePrompts?: unknown
    status: StepStatus
    rawResponse?: string
    createdAt?: string
}

export interface EpisodeExercisesDebugDto {
    id: string
    exercises?: unknown
    exerciseCount?: number
    vocabularyCoverage?: unknown
    grammarCoverage?: unknown
    status: StepStatus
    rawResponse?: string
    createdAt?: string
}

export interface CharacterDebugDto {
    id: string
    name: string
    role?: string
    gender?: string
    initialDescription?: string
    ageRange?: string
    personalityTraits?: unknown
    background?: string
    appearanceDescription?: string
    voiceId?: string
    referenceImageUrl?: string
    developments?: unknown
    createdAt?: string
    updatedAt?: string
}

export interface MediaDebugDto {
    id: string
    mediaType: string
    episodePlanId?: string
    characterId?: string
    url?: string
    metadata?: unknown
    status: StepStatus
    errorMessage?: string
    createdAt?: string
}

// Stage display configuration
export const STAGE_CONFIG: Record<GenerationStage, { label: string; icon: string; order: number }> = {
    BLUEPRINT: {label: 'Blueprint', icon: 'pi-file-edit', order: 0},
    MODULE_PLANNING: {label: 'Module Planning', icon: 'pi-list', order: 1},
    EPISODE_CONTENT: {label: 'Episode Content', icon: 'pi-book', order: 2},
    VOCABULARY_LINKING: {label: 'Vocabulary', icon: 'pi-bookmark', order: 3},
    CHARACTER_PROFILES: {label: 'Characters', icon: 'pi-users', order: 4},
    EXERCISES: {label: 'Exercises', icon: 'pi-check-square', order: 5},
    MEDIA: {label: 'Media', icon: 'pi-volume-up', order: 6},
    COMPLETED: {label: 'Completed', icon: 'pi-check-circle', order: 7},
    FAILED: {label: 'Failed', icon: 'pi-times-circle', order: -1}
}

export const LANGUAGE_OPTIONS = [
    {label: 'French', value: 'fr'},
    {label: 'German', value: 'de'},
    {label: 'Spanish', value: 'es'},
    {label: 'Italian', value: 'it'},
    {label: 'Portuguese', value: 'pt'}
]

export const CEFR_LEVELS = [
    {label: 'A1 - Beginner', value: 'A1'},
    {label: 'A2 - Elementary', value: 'A2'},
    {label: 'B1 - Intermediate', value: 'B1'},
    {label: 'B2 - Upper Intermediate', value: 'B2'},
    {label: 'C1 - Advanced', value: 'C1'}
]
