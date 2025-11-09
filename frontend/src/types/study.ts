// Study Mode Type Definitions

export type SessionSourceType = 'WORD_SET' | 'VOCABULARY' | 'DUE_REVIEW'
export type SessionStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED'
export type ItemStatus = 'NEW' | 'LEARNING' | 'COMPLETED'

// Request DTOs
export interface StartSessionRequest {
  source: SessionSourceType
  wordSetId?: number
  sessionSize: number
  includeNewWords: boolean
}

export interface AnswerRequest {
  cardId: number
  correct: boolean
  responseTimeMs?: number
}

// Response DTOs
export interface SessionProgressDto {
  completed: number
  learning: number
  new: number
}

export interface SessionStatsDto {
  totalAttempts: number
  correctAttempts: number
  incorrectAttempts: number
  accuracy: number
}

export interface StudySession {
  sessionId: number
  status: SessionStatus
  startedAt: string
  totalWords: number
  wordsCompleted: number
  progress: SessionProgressDto
  stats: SessionStatsDto
}

export interface WordDto {
  id: number
  lemma: string
  partOfSpeech: string
  languageCode: string
}

export interface DefinitionDto {
  id: number
  definitionNumber: number
  definitionText: string
  examples: ExampleDto[]
}

export interface ExampleDto {
  id: number
  sentenceText: string
  translation?: string
}

export interface WordDetailDto extends WordDto {
  definitions: DefinitionDto[]
}

export interface CardProgressDto {
  position: number
  total: number
  currentStreak: number
  needsStreak: number
}

export interface SrsInfoDto {
  reviewCount: number
  currentInterval: string
  nextReview?: string
}

export interface NextCardResponse {
  cardId: number
  word: WordDetailDto
  progress: CardProgressDto
  srsInfo: SrsInfoDto
}

export interface AnswerResponse {
  success: boolean
  consecutiveCorrect: number
  itemCompleted: boolean
  sessionCompleted: boolean
}

export interface SessionSummaryStats {
  totalWords: number
  newWords: number
  reviewWords: number
  totalAttempts: number
  correctAttempts: number
  incorrectAttempts: number
  accuracy: number
  averageResponseTime?: string
}

export interface SrsUpdatesSummaryDto {
  wordsAdvanced: number
  wordsReset: number
  nextDueCount: number
}

export interface SessionSummaryResponse {
  sessionId: number
  completedAt: string
  duration: string
  stats: SessionSummaryStats
  srsUpdates: SrsUpdatesSummaryDto
}

export interface DueWordsResponse {
  totalDue: number
  overdue: number
  dueToday: number
  dueSoon: number
}

export interface DueWordDto {
  wordId: number
  lemma: string
  partOfSpeech: string
  languageCode: string
  nextReviewAt: string
  daysOverdue: number
  reviewCount: number
  currentInterval: string
}

export interface DueWordsList {
  words: DueWordDto[]
  totalCount: number
}
