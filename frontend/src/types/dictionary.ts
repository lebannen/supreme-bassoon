/**
 * Dictionary API types for word lookup and search
 */

export interface Language {
    code: string
    name: string
    entryCount: number
}

export interface WordSummary {
    id: number
    lemma: string
    partOfSpeech: string | null
    frequencyRank: number | null
}

export interface SearchResult {
    words: WordSummary[]
    total: number
}

export interface Example {
    id: number
    sentenceText: string
    translation: string | null
}

export interface Definition {
    id: number
    definitionNumber: number
    definitionText: string
    examples: Example[]
}

export interface Pronunciation {
    id: number
    ipa: string | null
    audioUrl: string | null
    dialect: string | null
}

export interface InflectedForm {
    id: number
    form: string
    partOfSpeech: string | null
    grammaticalFeatures: Record<string, unknown> | null
}

export interface BaseForm {
    id: number
    lemma: string
    partOfSpeech: string | null
}

export interface Word {
    id: number
    languageCode: string
    lemma: string
    partOfSpeech: string | null
    etymology: string | null
    usageNotes: string | null
    frequencyRank: number | null
    isInflectedForm: boolean
    lemmaId: number | null
    grammaticalFeatures: Record<string, unknown> | null
    baseForm: BaseForm | null
    definitions: Definition[]
    pronunciations: Pronunciation[]
    inflectedForms: InflectedForm[]
}
