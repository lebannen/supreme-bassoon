import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiService } from '@/services/api'
import type {
  StudySession,
  NextCardResponse,
  SessionSummaryResponse,
  StartSessionRequest,
  DueWordsResponse
} from '@/types/study'

export const useStudyStore = defineStore('study', () => {
  const activeSession = ref<StudySession | null>(null)
  const currentCard = ref<NextCardResponse | null>(null)
  const sessionSummary = ref<SessionSummaryResponse | null>(null)
  const dueWords = ref<DueWordsResponse | null>(null)

  const isCardFlipped = ref(false)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const startTime = ref<number | null>(null)

  const isSessionActive = computed(() => !!activeSession.value && activeSession.value.status === 'ACTIVE')
  const progressPercentage = computed(() => {
    if (!activeSession.value) return 0
    return (activeSession.value.wordsCompleted / activeSession.value.totalWords) * 100
  })

  // Load active session on init
  async function loadActiveSession() {
    try {
      const session = await apiService.getActiveSession()
      if (session) {
        activeSession.value = session
      }
    } catch (err) {
      console.error('Failed to load active session:', err)
    }
  }

  // Load due words count
  async function loadDueWords() {
    try {
      dueWords.value = await apiService.getDueWords()
    } catch (err) {
      console.error('Failed to load due words:', err)
    }
  }

  // Start a new study session
  async function startSession(request: StartSessionRequest) {
    loading.value = true
    error.value = null

    try {
      const session = await apiService.startStudySession(request)
      activeSession.value = session
      sessionSummary.value = null

      // Get first card
      await fetchNextCard()

      return true
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to start session'
      return false
    } finally {
      loading.value = false
    }
  }

  // Get next card to study
  async function fetchNextCard() {
    if (!activeSession.value) {
      error.value = 'No active session'
      return
    }

    loading.value = true
    error.value = null
    startTime.value = Date.now()

    try {
      const card = await apiService.getNextCard(activeSession.value.sessionId)

      if (card) {
        currentCard.value = card
        isCardFlipped.value = false
      } else {
        // No more cards - session complete
        currentCard.value = null
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to get next card'
    } finally {
      loading.value = false
    }
  }

  // Flip the current card
  function flipCard() {
    isCardFlipped.value = !isCardFlipped.value
  }

  // Submit answer for current card
  async function submitAnswer(correct: boolean) {
    if (!activeSession.value || !currentCard.value) {
      error.value = 'No active session or card'
      return
    }

    loading.value = true
    error.value = null

    try {
      const responseTimeMs = startTime.value ? Date.now() - startTime.value : undefined

      const result = await apiService.submitAnswer(activeSession.value.sessionId, {
        cardId: currentCard.value.cardId,
        correct,
        responseTimeMs
      })

      // Update session stats
      const updatedSession = await apiService.getSession(activeSession.value.sessionId)
      activeSession.value = updatedSession

      if (result.sessionCompleted) {
        // Session is complete
        await completeSession()
      } else {
        // Get next card
        await fetchNextCard()
      }

      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to submit answer'
      return null
    } finally {
      loading.value = false
    }
  }

  // Complete the active session
  async function completeSession() {
    if (!activeSession.value) {
      error.value = 'No active session'
      return null
    }

    loading.value = true
    error.value = null

    try {
      const summary = await apiService.completeSession(activeSession.value.sessionId)

      sessionSummary.value = summary
      activeSession.value = null
      currentCard.value = null
      isCardFlipped.value = false

      // Reload due words count
      await loadDueWords()

      return summary
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to complete session'
      return null
    } finally {
      loading.value = false
    }
  }

  // Abandon the active session
  async function abandonSession() {
    if (!activeSession.value) {
      return
    }

    loading.value = true
    error.value = null

    try {
      await apiService.abandonSession(activeSession.value.sessionId)

      activeSession.value = null
      currentCard.value = null
      sessionSummary.value = null
      isCardFlipped.value = false
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to abandon session'
    } finally {
      loading.value = false
    }
  }

  // Clear session summary
  function clearSummary() {
    sessionSummary.value = null
  }

  return {
    // State
    activeSession,
    currentCard,
    sessionSummary,
    dueWords,
    isCardFlipped,
    loading,
    error,

    // Computed
    isSessionActive,
    progressPercentage,

    // Actions
    loadActiveSession,
    loadDueWords,
    startSession,
    fetchNextCard,
    flipCard,
    submitAnswer,
    completeSession,
    abandonSession,
    clearSummary
  }
})
