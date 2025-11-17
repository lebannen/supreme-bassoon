import {BaseAPI} from './base'
import type {
    AnswerRequest,
    AnswerResponse,
    DueWordsList,
    DueWordsResponse,
    NextCardResponse,
    SessionSummaryResponse,
    StartSessionRequest,
    StudySession,
} from '@/types/study'

/**
 * API client for spaced repetition study sessions
 */
export class StudyAPI extends BaseAPI {
    /**
     * Start a new study session
     */
    async startSession(request: StartSessionRequest): Promise<StudySession> {
        return this.post<StudySession>('/api/study/sessions/start', request)
    }

    /**
     * Get the active study session
     */
    async getActiveSession(): Promise<StudySession | null> {
        try {
            return await this.get<StudySession>('/api/study/sessions/active')
        } catch (error) {
            // Return null if no active session (404)
            if (error instanceof Error && error.message.includes('404')) {
                return null
            }
            throw error
        }
    }

    /**
     * Get a specific study session by ID
     */
    async getSession(sessionId: number): Promise<StudySession> {
        return this.get<StudySession>(`/api/study/sessions/${sessionId}`)
    }

    /**
     * Get the next card in the session
     */
    async getNextCard(sessionId: number): Promise<NextCardResponse | null> {
        try {
            return await this.get<NextCardResponse>(`/api/study/sessions/${sessionId}/next-card`)
        } catch (error) {
            // Return null if no more cards (204 No Content)
            if (error instanceof Error && error.message.includes('204')) {
                return null
            }
            throw error
        }
    }

    /**
     * Submit an answer for a card
     */
    async submitAnswer(sessionId: number, answer: AnswerRequest): Promise<AnswerResponse> {
        return this.post<AnswerResponse>(`/api/study/sessions/${sessionId}/answer`, answer)
    }

    /**
     * Complete the study session
     */
    async completeSession(sessionId: number): Promise<SessionSummaryResponse> {
        return this.post<SessionSummaryResponse>(`/api/study/sessions/${sessionId}/complete`)
    }

    /**
     * Abandon/cancel the study session
     */
    async abandonSession(sessionId: number): Promise<void> {
        return this.delete<void>(`/api/study/sessions/${sessionId}`)
    }

    /**
     * Get due words summary
     */
    async getDueWords(): Promise<DueWordsResponse> {
        return this.get<DueWordsResponse>('/api/study/due-words')
    }

    /**
     * Get list of due words
     */
    async getDueWordsList(limit: number = 50): Promise<DueWordsList> {
        return this.get<DueWordsList>(`/api/study/due-words/list?limit=${limit}`)
    }
}
