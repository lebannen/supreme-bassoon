export interface WordSet {
  id: number
  name: string
  description: string | null
  languageCode: string
  languageName: string
  level: string | null
  theme: string | null
  wordCount: number
  isImported: boolean
  userVocabularyCount: number
}

export interface WordSetDetail extends WordSet {
  words: WordSummary[]
}

export interface WordSummary {
  id: number
  lemma: string
  partOfSpeech: string | null
  frequencyRank: number | null
}

export interface ImportWordSetRequest {
  addNotes?: boolean
  notes?: string
}

export interface ImportWordSetResponse {
  wordSetId: number
  totalWords: number
  addedWords: number
  alreadyInVocabulary: number
  message: string
}
