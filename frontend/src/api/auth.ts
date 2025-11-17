import {BaseAPI} from './base'
import type {AuthResponse, LoginRequest, RegisterRequest, User} from '@/types/auth'

/**
 * API client for authentication endpoints
 */
export class AuthAPI extends BaseAPI {
    /**
     * Login with email and password
     */
    async login(credentials: LoginRequest): Promise<AuthResponse> {
        return this.post<AuthResponse>('/api/auth/login', credentials)
    }

    /**
     * Register a new user
     */
    async register(data: RegisterRequest): Promise<AuthResponse> {
        return this.post<AuthResponse>('/api/auth/register', data)
    }

    /**
     * Get current authenticated user
     */
    async getCurrentUser(): Promise<User> {
        return this.get<User>('/api/auth/me')
    }

    /**
     * Update user profile
     */
    async updateProfile(data: {
        displayName?: string
        nativeLanguage?: string
        learningLanguages?: string[]
    }): Promise<User> {
        return this.put<User>('/api/auth/profile', data)
    }
}
