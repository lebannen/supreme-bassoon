export interface User {
  id: number
  email: string
  displayName: string | null
  nativeLanguage: string
  learningLanguages: string[]
  roles: string[]
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  displayName?: string
}

export interface AuthResponse {
  token: string
  user: User
}
