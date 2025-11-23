import router from '@/router'

/**
 * Base API client with automatic error handling and authentication
 */
export class BaseAPI {
    protected baseURL: string

    constructor() {
        this.baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    }

    /**
     * Get headers for authenticated requests
     */
    protected getHeaders(additionalHeaders?: HeadersInit): HeadersInit {
        const headers: HeadersInit = {
            'Content-Type': 'application/json',
            ...additionalHeaders,
        }

        // Add authorization header if token exists
        const token = localStorage.getItem('auth_token')
        if (token) {
            headers['Authorization'] = `Bearer ${token}`
        }

        return headers
    }

    /**
     * Make a type-safe HTTP request
     */
    protected async request<T>(
        endpoint: string,
        options: RequestInit = {}
    ): Promise<T> {
        const url = `${this.baseURL}${endpoint}`

        try {
            const response = await fetch(url, {
                ...options,
                headers: this.getHeaders(options.headers),
            })

            // Handle 401 Unauthorized - session expired
            if (response.status === 401) {
                await this.handle401()
                throw new Error('Authentication required')
            }

            // Handle 404 Not Found
            if (response.status === 404) {
                throw new Error('Resource not found')
            }

            // Handle other HTTP errors
            if (!response.ok) {
                const errorText = await response.text()
                let errorMessage = `HTTP ${response.status}: ${response.statusText}`

                try {
                    const errorJson = JSON.parse(errorText)
                    errorMessage = errorJson.message || errorJson.error || errorMessage
                } catch {
                    // If response is not JSON, use the text
                    if (errorText) {
                        errorMessage = errorText
                    }
                }

                throw new Error(errorMessage)
            }

            // Handle 204 No Content
            if (response.status === 204) {
                return null as T
            }

            return await response.json()
        } catch (error) {
            // Re-throw the error for the caller to handle
            throw error
        }
    }

    /**
     * Handle 401 Unauthorized responses
     */
    private async handle401() {
        // Dynamic import to avoid circular dependency
        const { useAuthStore } = await import('@/stores/auth')
        const authStore = useAuthStore()

        // Only log out if we're currently authenticated
        if (authStore.isAuthenticated) {
            console.warn('Session expired, logging out...')
            authStore.logout()

            // Redirect to login with the current path for redirect after login
            router.push({
                name: 'login',
                query: {
                    redirect: router.currentRoute.value.fullPath,
                    expired: 'true',
                },
            })
        }
    }

    /**
     * GET request
     */
    protected async get<T>(endpoint: string, options?: RequestInit): Promise<T> {
        return this.request<T>(endpoint, {
            ...options,
            method: 'GET',
        })
    }

    /**
     * POST request
     */
    protected async post<T>(
        endpoint: string,
        data?: unknown,
        options?: RequestInit
    ): Promise<T> {
        return this.request<T>(endpoint, {
            ...options,
            method: 'POST',
            body: data ? JSON.stringify(data) : undefined,
        })
    }

    /**
     * PUT request
     */
    protected async put<T>(
        endpoint: string,
        data?: unknown,
        options?: RequestInit
    ): Promise<T> {
        return this.request<T>(endpoint, {
            ...options,
            method: 'PUT',
            body: data ? JSON.stringify(data) : undefined,
        })
    }

    /**
     * PATCH request
     */
    protected async patch<T>(
        endpoint: string,
        data?: unknown,
        options?: RequestInit
    ): Promise<T> {
        return this.request<T>(endpoint, {
            ...options,
            method: 'PATCH',
            body: data ? JSON.stringify(data) : undefined,
        })
    }

    /**
     * DELETE request
     */
    protected async delete<T>(endpoint: string, options?: RequestInit): Promise<T> {
        return this.request<T>(endpoint, {
            ...options,
            method: 'DELETE',
        })
    }
}
