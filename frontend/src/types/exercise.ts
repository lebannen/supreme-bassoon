export interface ExerciseSummary {
  id: number
  type: string
  title: string
  languageCode: string
  moduleNumber: number | null
  topic: string | null
  difficultyRating: number
  estimatedDurationSeconds: number
  pointsValue: number
}

export interface Exercise {
  id: number
  type: string
  languageCode: string
  moduleNumber: number | null
  topic: string | null
  cefrLevel: string
  title: string
  instructions: string
    content: any // Will vary by exercise type
  estimatedDurationSeconds: number
  pointsValue: number
  difficultyRating: number
}

export interface SubmitAttemptRequest {
  userResponses: any
  durationSeconds: number | null
  hintsUsed: number
}

export interface AttemptResult {
  attemptId: number
  isCorrect: boolean
  score: number
  feedback: string
  correctAnswers: any | null
  completedAt: string
}

export interface UserProgress {
  exerciseId: number
  attemptsCount: number
  bestScore: number | null
  lastAttemptAt: string | null
  isCompleted: boolean
}
