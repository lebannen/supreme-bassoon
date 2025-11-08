export interface WordSummary {
  id: number
  lemma: string
  partOfSpeech: string | null
  frequencyRank: number | null
}

export interface VocabularyWord {
  vocabularyId: number
  word: WordSummary
  notes: string | null
  addedAt: string
}

export interface AddWordToVocabularyRequest {
  wordId: number
  notes?: string
}
