/**
 * Centralized API clients
 *
 * All API communication should go through these clients.
 * Each client extends BaseAPI and provides automatic:
 * - Authentication token injection
 * - 401 error handling with auto-logout
 * - Centralized error handling
 * - Type-safe requests
 *
 * Usage:
 * ```ts
 * import { authAPI, courseAPI } from '@/api'
 *
 * const user = await authAPI.getCurrentUser()
 * const courses = await courseAPI.getCourses()
 * ```
 */

import {AuthAPI} from './auth'
import {CourseAPI} from './course'
import {DictionaryAPI} from './dictionary'
import {ExerciseAPI} from './exercise'
import {ReadingAPI} from './reading'
import {StudyAPI} from './study'
import {VocabularyAPI} from './vocabulary'
import {WordSetAPI} from './wordSet'

// Export singleton instances for use throughout the application
export const authAPI = new AuthAPI()
export const courseAPI = new CourseAPI()
export const dictionaryAPI = new DictionaryAPI()
export const exerciseAPI = new ExerciseAPI()
export const readingAPI = new ReadingAPI()
export const studyAPI = new StudyAPI()
export const vocabularyAPI = new VocabularyAPI()
export const wordSetAPI = new WordSetAPI()

// Also export the classes for testing or advanced use cases
export {AuthAPI} from './auth'
export {CourseAPI} from './course'
export {DictionaryAPI} from './dictionary'
export {ExerciseAPI} from './exercise'
export {ReadingAPI} from './reading'
export {StudyAPI} from './study'
export {VocabularyAPI} from './vocabulary'
export {WordSetAPI} from './wordSet'
