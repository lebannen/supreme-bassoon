# Development Instructions for AI Assistants

## Backend Management
- **NEVER start or restart the backend** - The user manages this manually!
- **NEVER start or restart the backend** - The user manages this manually!
- **NEVER start or restart the backend** - The user manages this manually!
- Backend runs on port 8080 via `./gradlew bootRun`
- Only make code changes to backend files, never execute gradle commands to run it
- If backend changes are made, inform the user but let them restart it themselves

## Frontend Management
- **NEVER start or restart the frontend** - The user manages this manually
- Frontend development server can be managed as needed
- Runs on a separate port from backend

## Database
- PostgreSQL database at localhost:5432/vocabee
- User manages database operations
- Flyway migrations are applied automatically on backend startup

## Code Style Preferences

### Kotlin
- Use data classes for DTOs and value objects
- Constructor injection (primary constructor parameter)
- Explicit types for public API, inference for local variables
- Named parameters for @Query annotations
- No !! operator (prefer safe calls ?. and elvis ?: )

### TypeScript/Vue
- Explicit types for function parameters and return values
- Prefer `const` over `let`
- No emojis unless explicitly requested

### SQL
- Snake_case for table and column names
- Uppercase for SQL keywords
- Always use named parameters in JPQL (`:paramName`)

## When to Ask vs. When to Proceed

### ASK First:
- Architecture changes
- New dependencies
- Database schema changes (new tables, columns, indexes)
- Major refactoring that affects multiple modules
- Performance optimization approaches
- Changes to API contracts

### PROCEED Without Asking:
- Bug fixes within existing patterns
- Refactoring within a single class/file
- Adding tests
- Code formatting and style improvements
- Adding comments/documentation
- Implementing agreed-upon features

## Communication Preferences
- Show actual code changes, not just descriptions
- Explain "why" for non-obvious decisions
- When debugging: show relevant logs/data before proposing fixes
- No emojis unless explicitly requested
- Use TodoWrite tool for multi-step tasks
- Mark todos as completed immediately after finishing

## Error Handling Philosophy
- Always log errors with context (importId, word lemma, etc.)
- Fail gracefully with word-by-word retry on batch errors
- Never silently swallow exceptions in import pipelines
- Log both error class and message for debugging

## General Guidelines
- Read context.md and testing.md before starting work
- Follow patterns established in ARCHITECTURE.md
- Make code changes as requested
- Provide clear summaries of what was changed
- Let the user control when to restart services
