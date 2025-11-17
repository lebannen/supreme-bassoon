# Backend Integration TODOs

This file tracks all the places where we need to connect to the backend API.

## Dashboard Store (`src/stores/dashboard.ts`)

### User Statistics

- **File**: `src/stores/dashboard.ts`
- **Function**: `fetchUserStats()`
- **Endpoint**: `GET /api/user/stats`
- **Response Shape**:
  ```typescript
  {
    streak: number,
    wordsLearned: number,
    timeThisWeek: string  // e.g., "3h 24m"
  }
  ```
- **Status**: ❌ Not implemented
- **Priority**: High
- **Notes**: Currently using mock data. Replace the mock delay with actual API call.

### Daily Tasks

- **File**: `src/stores/dashboard.ts`
- **Function**: `fetchDailyTasks()`
- **Endpoint**: `GET /api/user/daily-tasks`
- **Response Shape**:
  ```typescript
  Array<{
    id: number,
    icon: string,
    title: string,
    meta: string,
    completed: boolean,
    type?: 'vocabulary' | 'exercise' | 'listening' | 'grammar'
  }>
  ```
- **Status**: ❌ Not implemented
- **Priority**: High
- **Notes**: Tasks should be personalized based on user progress and learning goals.

### Recommended Content

- **File**: `src/stores/dashboard.ts`
- **Function**: `fetchRecommendedContent()`
- **Endpoint**: `GET /api/user/recommended-content`
- **Response Shape**:
  ```typescript
  Array<{
    id: number,
    type: 'dialogue' | 'story' | 'grammar',
    icon: string,
    title: string,
    level?: string,
    duration?: string,
    topic?: string,
    completed?: boolean
  }>
  ```
- **Status**: ❌ Not implemented
- **Priority**: Medium
- **Notes**: Recommendations should be based on user's level, interests, and learning history.

### Complete Task

- **File**: `src/stores/dashboard.ts`
- **Function**: `completeTask(taskId: number)`
- **Endpoint**: `POST /api/user/tasks/:taskId/complete`
- **Request Body**: None (task ID in URL)
- **Response**: Success/error status
- **Status**: ❌ Not implemented
- **Priority**: High
- **Notes**: Currently updates local state only. Should sync with backend and update user progress.

## Home View (`src/views/HomeView.vue`)

### Task Navigation

- **File**: `src/views/HomeView.vue`
- **Function**: `handleTaskAction(taskId: number)`
- **TODO**: Navigate to appropriate view based on task type
    - `vocabulary` → `/vocabulary`
    - `exercise` → `/exercises`
    - `listening` → `/reading` or content player
    - `grammar` → grammar reference page
- **Status**: ❌ Not implemented
- **Priority**: Medium
- **Notes**: Need to determine the exact routes for each task type.

### Content Navigation

- **File**: `src/views/HomeView.vue`
- **Function**: `handleContentClick(contentId: number)`
- **TODO**: Navigate to specific content page based on content ID
- **Status**: ❌ Partially implemented (goes to /courses)
- **Priority**: Medium
- **Notes**: Should navigate to the actual content detail page (dialogue/story/grammar).

## API Integration Checklist

When implementing each endpoint:

1. **Create API Service**
    - [ ] Add endpoint to `src/services/api.ts` or create `src/composables/useDashboardApi.ts`
    - [ ] Handle authentication (Bearer token)
    - [ ] Handle errors gracefully

2. **Update Store**
    - [ ] Replace mock data with API call
    - [ ] Add proper error handling
    - [ ] Add loading states
    - [ ] Handle edge cases (no data, empty arrays, etc.)

3. **Test**
    - [ ] Test with real backend data
    - [ ] Test error states
    - [ ] Test loading states
    - [ ] Test authentication required scenarios

## Future Enhancements

### Dashboard Features to Add

- [ ] Weekly/monthly progress charts
- [ ] Achievement badges
- [ ] Learning streak history
- [ ] Personalized learning goals
- [ ] Recent activity feed
- [ ] Upcoming lessons/tasks preview

### Data Refresh

- [ ] Implement real-time updates (WebSocket or polling)
- [ ] Add pull-to-refresh
- [ ] Cache dashboard data with expiry
- [ ] Optimistic UI updates for better UX

## Notes

- All endpoints should require authentication
- Use consistent error handling across all API calls
- Consider adding retry logic for failed requests
- Add analytics tracking for user interactions
- Implement proper TypeScript types for all API responses
