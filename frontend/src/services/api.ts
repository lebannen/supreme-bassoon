import type { LoginRequest, RegisterRequest, AuthResponse, User } from '@/types/auth'
import type { VocabularyWord, AddWordToVocabularyRequest } from '@/types/vocabulary'

const API_BASE_URL = 'http://localhost:8080/api'

class ApiService {
  private getHeaders(includeAuth = false): HeadersInit {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    }

    if (includeAuth) {
      const token = localStorage.getItem('auth_token')
      if (token) {
        headers['Authorization'] = `Bearer ${token}`
      }
    }

    return headers
  }

  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(credentials),
    })

    if (!response.ok) {
      throw new Error('Login failed')
    }

    return response.json()
  }

  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      throw new Error('Registration failed')
    }

    return response.json()
  }

  async getCurrentUser(): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      method: 'GET',
      headers: this.getHeaders(true),
    })

    if (!response.ok) {
      throw new Error('Failed to get current user')
    }

    return response.json()
  }

  async updateProfile(data: {
    displayName?: string
    nativeLanguage?: string
    learningLanguages?: string[]
  }): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/auth/profile`, {
      method: 'PUT',
      headers: this.getHeaders(true),
      body: JSON.stringify(data),
    })

    if (!response.ok) {
      throw new Error('Failed to update profile')
    }

    return response.json()
  }

  async getUserVocabulary(): Promise<VocabularyWord[]> {
    const response = await fetch(`${API_BASE_URL}/vocabulary`, {
      method: 'GET',
      headers: this.getHeaders(true),
    })

    if (!response.ok) {
      throw new Error('Failed to get user vocabulary')
    }

    return response.json()
  }

  async addWordToVocabulary(request: AddWordToVocabularyRequest): Promise<VocabularyWord> {
    const response = await fetch(`${API_BASE_URL}/vocabulary`, {
      method: 'POST',
      headers: this.getHeaders(true),
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error('Failed to add word to vocabulary')
    }

    return response.json()
  }

  async removeWordFromVocabulary(wordId: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/vocabulary/${wordId}`, {
      method: 'DELETE',
      headers: this.getHeaders(true),
    })

    if (!response.ok) {
      throw new Error('Failed to remove word from vocabulary')
    }
  }

  async checkIfWordInVocabulary(wordId: number): Promise<{ inVocabulary: boolean }> {
    const response = await fetch(`${API_BASE_URL}/vocabulary/check/${wordId}`, {
      method: 'GET',
      headers: this.getHeaders(true),
    })

    if (!response.ok) {
      throw new Error('Failed to check if word in vocabulary')
    }

    return response.json()
  }
}

export const apiService = new ApiService()
