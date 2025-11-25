# Vocabee Platform Redesign - Requirements Document

**Version:** 2.0
**Date:** January 16, 2025
**Purpose:** Complete functional requirements for UI/UX redesign
**Target Audience:** Design team / Fresh implementation perspective

---

## Executive Summary

Vocabee is a language learning platform focused on **practical language acquisition through multiple engagement methods
**. Unlike traditional linear course platforms, Vocabee emphasizes daily engagement, content discovery, and flexible
learning paths.

### Core Philosophy

1. **Daily Engagement Over Long Commitments** - Quick, achievable daily tasks that build habits
2. **Multiple Learning Paths** - Structured courses AND self-directed exploration
3. **Content Reusability** - Same content accessible through multiple contexts
4. **Reference-Based Learning** - Grammar and vocabulary as accessible references, not blockers
5. **Real Content Focus** - Authentic dialogues, stories, and conversations

---

## Platform Vision

### Primary Goal

Enable language learners to build practical skills through:

- Daily micro-tasks (10-15 minutes)
- Discoverable authentic content (dialogues, stories)
- Structured learning paths (optional courses)
- Spaced repetition vocabulary practice
- Grammar reference when needed

### User Value Proposition

"Learn French naturally through daily practice, real conversations, and structured courses - all in one place."

---

## User Personas

### Primary Persona: Sarah (Self-Directed Learner)

- **Age:** 28, working professional
- **Goal:** Learn French for travel and personal growth
- **Time:** 15-20 minutes per day, inconsistent schedule
- **Needs:** Quick daily tasks, interesting content, visible progress
- **Pain Points:** Gets bored with rigid courses, needs flexibility

### Secondary Persona: Mike (Structured Learner)

- **Age:** 35, planning to move to France
- **Goal:** Reach A1 proficiency in 3 months
- **Time:** 1 hour daily, dedicated study time
- **Needs:** Clear curriculum, progress tracking, comprehensive coverage
- **Pain Points:** Wants structure but also practice variety

### Tertiary Persona: Lisa (Casual Explorer)

- **Age:** 42, hobbyist
- **Goal:** Maintain French skills from high school
- **Time:** Sporadic, whenever interested
- **Needs:** Interesting content, no pressure, discovery
- **Pain Points:** Doesn't want commitments, just wants to browse

---

## Content Types & Data Model

### 1. Vocabulary

**Description:** Individual words with definitions, translations, examples, pronunciation

**Attributes:**

- Word (lemma form)
- Translation(s)
- Part of speech
- Example sentences
- Audio pronunciation (AI-generated)
- Difficulty level (A1-C2)
- Related words
- User-specific data: added date, review history, notes

**User Actions:**

- Search words
- Save to personal vocabulary
- Review with spaced repetition
- Add personal notes

### 2. Dialogues

**Description:** Short conversations (2-5 speakers) with audio, transcript, vocabulary

**Attributes:**

- Title
- Topic/theme (e.g., "At the Restaurant", "Making Friends")
- Speakers (character names)
- Transcript (with speaker labels)
- Duration (2-5 minutes)
- Difficulty level (A1-C2)
- Multi-speaker audio (AI-generated, distinct voices)
- Key vocabulary list (10-20 words)
- Can be standalone OR part of a course module

**User Actions:**

- Play audio
- Read transcript (toggle visibility)
- Click words in transcript for definitions
- Save words from dialogue
- Mark as complete

### 3. Stories

**Description:** Short narratives for reading/listening practice with audio

**Attributes:**

- Title
- Text content (2-10 paragraphs)
- Duration (3-10 minutes)
- Difficulty level (A1-C2)
- Single-speaker narration audio (AI-generated)
- Key vocabulary list
- Topic/genre
- Can be standalone OR part of a course module

**User Actions:**

- Read text
- Listen to narration
- Click words for definitions
- Save words from story
- Mark as complete

### 4. Grammar Rules

**Description:** Reference documentation for grammar topics with examples and practice

**Attributes:**

- Topic name (e.g., "Passé Composé", "Definite Articles")
- Category (tenses, articles, pronouns, etc.)
- Difficulty level
- Explanation text
- Example sentences with translations
- Conjugation tables (for verbs)
- Related topics
- Optional: practice exercises

**Organization:**

- Hierarchical (category > topic > sub-topic)
- Searchable
- Cross-referenced

**User Actions:**

- Browse by category
- Search topics
- Read explanations
- View examples
- Practice (if exercises available)
- Bookmark favorites

### 5. Exercises

**Description:** Interactive practice activities with immediate feedback

**Exercise Types:**

- Multiple Choice (select correct answer from options)
- Fill-in-Blank (type missing word in sentence)
- Sentence Scramble (reorder words to form sentence)
- Matching (connect pairs, e.g., word to translation)
- Listening Comprehension (listen to audio, answer question)
- Cloze Reading (fill multiple blanks in paragraph)

**Common Attributes:**

- Type
- Title
- Instructions
- Content (varies by type)
- Difficulty level
- Estimated time (1-3 minutes)
- Points value
- Hint (optional)
- Explanation/feedback

**User Actions:**

- Attempt exercise
- Submit answer
- See immediate feedback (correct/incorrect)
- View explanation
- Retry if incorrect
- Auto-advance to next if correct (with 3-second countdown)

### 6. Courses

**Description:** Structured learning paths organized into modules and episodes

**Structure:**

- **Course** (e.g., "French A1: Sophie's Parisian Journey")
    - Contains 5-15 **Modules**
        - Each module contains 2-4 **Episodes**
            - Each episode contains **Content Items** (dialogues, stories, grammar rules, exercises)

**Course Attributes:**

- Name
- Description
- Language & CEFR level
- Estimated total hours
- Number of modules
- Total exercises

**Module Attributes:**

- Module number
- Title (e.g., "Greetings & Introductions")
- Description
- Number of episodes

**Episode Attributes:**

- Title
- Type (Dialogue, Story, Article, Audio Lesson)
- Content (dialogue or story)
- Audio
- Ordered list of content items (grammar rules, exercises)

**User Progress:**

- Track completed episodes
- Track completed exercises within episodes
- Overall course completion percentage
- Resume from last incomplete item

**User Actions:**

- Browse available courses
- Enroll in course
- Navigate modules and episodes
- Complete content items in sequence
- Track progress

### 7. Vocabulary Sets

**Description:** Curated word lists for specific topics or levels

**Attributes:**

- Name (e.g., "Essential A1 Verbs", "Restaurant Vocabulary")
- Description
- Level
- Word list (20-100 words)
- Source (system-curated or user-created)

**User Actions:**

- Browse sets
- Add entire set to personal vocabulary
- Create custom sets

---

## Functional Requirements by Feature Area

### Feature Area 1: Daily Tasks

**Purpose:** Provide a focused, achievable daily practice routine

**Requirements:**

1. System generates personalized daily task list (4-6 items)
2. Task types include:
    - Vocabulary review (spaced repetition)
    - Random exercises (2-3)
    - Content recommendation (dialogue or story)
    - Grammar tip of the day
3. Each task shows estimated time
4. Tasks marked as complete when finished
5. Streak tracking (consecutive days with tasks completed)
6. Task list resets daily
7. Users can start any task with one click
8. Progress indicator shows tasks completed / total

**User Flows:**

1. User lands on platform → sees today's tasks
2. Clicks "Start" on a task → begins that activity
3. Completes task → returns to task list, sees checkmark
4. Completes all tasks → sees congratulations, streak updated
5. Returns next day → new task list generated

### Feature Area 2: Content Library

**Purpose:** Browse and discover standalone content (dialogues, stories, grammar)

**Requirements:**

**2.1 Library Organization**

1. Content organized into sections: Dialogues, Stories, Grammar
2. Each section browseable independently
3. Filtering by:
    - Difficulty level (A1, A2, B1, B2, C1, C2)
    - Topic/theme
    - Duration
4. Search across all content
5. Sort by: most recent, most popular, level

**2.2 Content Display**

1. Grid layout for visual browsing
2. Each item shows:
    - Cover image or illustration
    - Title
    - Type (Dialogue/Story/Grammar)
    - Level badge
    - Duration
    - Audio indicator (if has audio)
    - Completion status (if completed)
3. Click to view details/play

**2.3 Content Consumption**

1. Dedicated view for each content type
2. Dialogues/Stories:
    - Audio player (play/pause, progress bar, speed control)
    - Transcript (toggle visibility)
    - Interactive text (click words for definitions)
    - Vocabulary extraction (list of key words)
    - Save words to personal vocabulary
    - Mark as complete
    - Next/previous navigation (if in sequence)
3. Grammar:
    - Structured text presentation
    - Examples highlighted
    - Conjugation tables (for verbs)
    - Related topics linked
    - Practice exercises linked (if available)

**User Flows:**

1. User navigates to Library
2. Selects section (Dialogues/Stories/Grammar)
3. Filters by level/topic
4. Clicks content item
5. Consumes content (reads/listens)
6. Saves useful words
7. Marks complete
8. Returns to library or continues to next item

### Feature Area 3: Structured Courses

**Purpose:** Provide guided, sequential learning paths

**Requirements:**

**3.1 Course Browsing**

1. List of available courses
2. Each course shows:
    - Cover image
    - Title
    - Language & level
    - Number of modules
    - Total estimated hours
    - Enrollment status
3. Click to view details

**3.2 Course Navigation**

1. Course overview shows:
    - Description
    - Progress (if enrolled)
    - List of modules
2. Module list shows:
    - Module number and title
    - Number of episodes
    - Completion status (not started / in progress / completed)
    - Locked state (if sequential)
3. Clicking module expands to show episodes
4. Clicking episode opens episode view

**3.3 Episode Experience**

1. Episode shows:
    - Episode content (dialogue or story)
    - Audio player
    - Transcript
    - Sequence of content items below:
        - Grammar rules
        - Exercises
2. Content items completed in order
3. Progress tracked per episode
4. Can resume from last incomplete item
5. Navigation: previous/next episode

**User Flows:**

1. User browses courses
2. Clicks course → sees overview and modules
3. Clicks module → expands to show episodes
4. Clicks episode → opens episode view
5. Reads/listens to main content
6. Proceeds through grammar and exercises
7. Completes episode → progress saved
8. Continues to next episode or exits

### Feature Area 4: Vocabulary Management

**Purpose:** Build and review personal vocabulary with spaced repetition

**Requirements:**

**4.1 Vocabulary Collection**

1. Users can save words from:
    - Content (dialogues, stories)
    - Exercises
    - Direct search/browse
2. Each saved word includes:
    - Word and translation
    - Source (where it was encountered)
    - Personal notes (optional)
    - Added date
    - Review history

**4.2 Vocabulary Browsing**

1. List view of all saved words
2. Search saved words
3. Filter by:
    - Level
    - Source
    - Due for review
4. Sort by:
    - Recently added
    - Alphabetical
    - Next review date

**4.3 Study Sessions**

1. Multiple session types:
    - Quick Review (5 words, ~2 min)
    - Due Reviews (all words due today)
    - All Words (comprehensive review)
    - Weak Words (struggled words only)
2. Session shows:
    - Word count
    - Estimated time
    - "Start" button
3. Review interface:
    - Shows word (or translation, randomized)
    - User attempts to recall
    - Reveals answer
    - User rates difficulty (Easy/Good/Hard/Again)
    - System schedules next review based on spaced repetition algorithm

**User Flows:**

1. User encounters word in content
2. Clicks word → sees definition popup
3. Clicks "Save to vocabulary"
4. Word added to personal list
5. Later: User goes to Vocabulary
6. Sees word is due for review
7. Starts review session
8. Reviews words, rates recall
9. System schedules next reviews

### Feature Area 5: Progress & Profile

**Purpose:** Track learning progress and manage settings

**Requirements:**

**5.1 Progress Dashboard**

1. Overall stats:
    - Current level progress (e.g., "A1 - 78% complete")
    - Total words learned
    - Study streak
    - This week's activity (exercises, words, time)
2. Course progress (if enrolled)
3. Recent activity log

**5.2 Profile Settings**

1. User information
2. Learning language selection
3. Native language
4. Daily goal settings (optional)
5. Notification preferences
6. Audio settings (autoplay, speed)

**User Flows:**

1. User accesses profile
2. Views stats and progress
3. Adjusts settings
4. Saves changes

### Feature Area 6: Admin Functions

**Purpose:** Content management for administrators

**Requirements:**

1. Separate admin interface (accessible only to admin users)
2. Course import/management
3. Content creation/editing
4. User management
5. Analytics dashboard

**Note:** Admin UI can have different design - focus user-facing mockups first.

---

## User Flows (Key Journeys)

### Flow 1: Daily Practice Session (Primary Flow)

**Persona:** Sarah (Self-Directed Learner)

**Scenario:** Sarah has 15 minutes before work

1. Opens Vocabee → lands on Home/Daily Tasks
2. Sees task list:
    - ✓ Vocabulary Review (completed yesterday evening)
    - ○ Daily Exercises (3 exercises)
    - ○ Quick Listen: "At the Café"
    - ○ Grammar Tip: Present Tense Review
3. Clicks "Start" on Daily Exercises
4. Completes 3 exercises (multiple choice, fill-in-blank, listening)
    - Each exercise shows feedback
    - Auto-advances after correct answers (3s countdown)
5. Returns to task list → sees exercises checked off
6. Starts "Quick Listen"
7. Plays dialogue, reads along
8. Clicks a word she doesn't know → sees definition
9. Saves word to vocabulary
10. Marks dialogue complete
11. Returns to task list
12. Sees progress: 3/4 tasks complete, 12 minutes spent
13. Closes app (will finish tomorrow)

**Key Touchpoints:** Home, Exercises, Content Player, Vocabulary Save

---

### Flow 2: Content Discovery & Learning

**Persona:** Lisa (Casual Explorer)

**Scenario:** Lisa is curious about French conversations

1. Opens Vocabee → clicks "Library"
2. Clicks "Dialogues" tab
3. Filters by "A1" level and "Food & Dining" topic
4. Browses grid of dialogue cards
5. Clicks "Ordering at a Restaurant"
6. Opens dialogue player
    - Sees cover image, title, metadata
    - Clicks "Play Audio"
7. Listens to conversation
8. Toggles transcript visibility on
9. Reads along while listening
10. Clicks word "addition" (the bill) → definition popup
11. Finds it useful → clicks "Save to vocabulary"
12. Continues listening
13. Finishes dialogue → clicks "Mark as Complete"
14. Sees recommended next dialogue
15. Clicks to continue or returns to library

**Key Touchpoints:** Library Browse, Content Filtering, Content Player, Word Definitions, Vocabulary Save

---

### Flow 3: Structured Course Learning

**Persona:** Mike (Structured Learner)

**Scenario:** Mike is working through French A1 course

1. Opens Vocabee → clicks "Courses"
2. Sees "French A1: Sophie's Parisian Journey" (enrolled)
3. Sees progress: Module 2, Episode 1 - 45% complete overall
4. Clicks "Continue" → opens current episode
5. Episode: "At the Hotel - Checking In"
    - Main content: Dialogue with Sophie checking into hotel
6. Plays audio, reads transcript
7. Saves 3 new words from dialogue
8. Scrolls to episode content items:
    - Grammar Rule: "Polite expressions with 'voudrais'"
    - Exercise 1: Multiple choice on hotel vocabulary
    - Exercise 2: Fill-in-blank with polite phrases
    - Exercise 3: Listening comprehension
9. Clicks grammar rule → reads explanation, views examples
10. Returns to episode → starts Exercise 1
11. Completes all 3 exercises
    - Progress indicator updates after each
12. Episode marked complete
13. Sees "Next Episode: Room Service"
14. Clicks next → continues learning

**Key Touchpoints:** Course Navigation, Episode View, Grammar Reference, Exercises, Progress Tracking

---

### Flow 4: Vocabulary Review Session

**Persona:** Sarah (Self-Directed Learner)

**Scenario:** Sarah sees she has 12 words due for review

1. Opens Vocabee → Home shows "Vocabulary Review - 12 words due"
2. Clicks "Start Review"
3. Review interface loads
    - Shows progress: 0/12
4. Card 1: Shows "hello" in English
    - Sarah thinks of French word
    - Clicks "Reveal"
    - Sees "bonjour"
    - Rates: "Easy"
5. Card 2: Shows "merci"
    - Sarah thinks of English
    - Clicks "Reveal"
    - Sees "thank you"
    - Remembers well → Rates: "Good"
6. Card 3: Shows "table" in English
    - Sarah not sure
    - Clicks "Reveal"
    - Sees "la table" (correct)
    - Didn't remember well → Rates: "Hard"
7. Continues through all 12 words
8. Session complete → sees summary:
    - 12 words reviewed
    - 8 Easy, 3 Good, 1 Hard
    - Next review: 8 words tomorrow
9. Returns to Home

**Key Touchpoints:** Vocabulary Review Interface, Spaced Repetition Ratings

---

## Design Principles & Constraints

### Design Principles

1. **Clarity Over Cleverness**
    - Clear labels and actions
    - Obvious navigation
    - No hidden features

2. **Content First**
    - Minimize UI chrome
    - Focus on learning content
    - Reduce distractions

3. **Progressive Disclosure**
    - Show what's needed, when it's needed
    - Don't overwhelm with options
    - Expand details on demand

4. **Consistent Patterns**
    - Same actions work the same way everywhere
    - Predictable layouts
    - Familiar interactions

5. **Visual Hierarchy**
    - Clear importance levels
    - Guide user's eye
    - Logical reading order

6. **Encouraging, Not Pushy**
    - Celebrate progress
    - Gentle reminders
    - No guilt-tripping

### Technical Constraints

**Platform:**

- Web application (primary)
- Desktop and tablet focus (mobile later)
- Modern browsers (Chrome, Firefox, Safari, Edge)

**Audio:**

- All dialogues and stories have audio available (AI-generated)
- Audio can be played, paused, speed-controlled
- Audio is NOT always visible - minimize player UI until needed
- Clear indicator when audio is available (icon)
- When playing, show more detailed controls

**Images:**

- Content items (courses, dialogues, stories) should have cover images
- Images make content more appealing and easier to scan
- Use illustrations or AI-generated images (not critical for mockups - placeholders OK)

**Performance:**

- Quick load times expected
- Instant feedback on interactions
- Smooth transitions

### UX Constraints

**Information Density:**

- Spacious design (generous whitespace)
- Don't cram too much on screen
- Readable text sizes
- Comfortable click/tap targets

**Navigation:**

- Maximum 2-3 clicks to reach any feature
- Clear "back" paths
- Breadcrumbs for deep hierarchies (courses → modules → episodes)

**Feedback:**

- Immediate response to user actions
- Clear success/error states
- Progress always visible

**Accessibility:**

- Keyboard navigable
- Screen reader friendly (semantic HTML)
- Sufficient color contrast
- No color-only indicators

### Non-Negotiables

1. **Daily Tasks must be front and center**
    - Not buried in navigation
    - The first thing users see (or one click away)

2. **Audio must be minimally intrusive**
    - Don't show full player always
    - Show indicator that audio exists
    - Expand controls when playing

3. **Progress must persist**
    - All completions saved
    - Resume from where left off
    - Never lose progress

4. **Multiple entry points to content**
    - Same dialogue accessible from:
        - Daily tasks
        - Library
        - Course episode
        - Recommendations

5. **Grammar is reference, not sequential**
    - Can be accessed anytime
    - Not blocking progress
    - Searchable and browseable

---

## Key Screens to Mock Up

### Priority 1 (Must Have)

1. **Home / Daily Tasks**
    - The landing page
    - Shows today's task list
    - Quick stats (streak, progress)
    - Recommendations

2. **Library - Content Browse**
    - Grid of content items (dialogues/stories/grammar)
    - Filters and search
    - Clear visual hierarchy

3. **Content Player (Dialogue/Story)**
    - Audio playback
    - Transcript
    - Word interaction
    - Vocabulary save

4. **Exercise View**
    - Unified interface for all exercise types
    - Question display
    - Answer input/selection
    - Feedback and auto-advance

5. **Vocabulary Management**
    - List of saved words
    - Search/filter
    - Study session options

### Priority 2 (Important)

6. **Course Overview**
    - Course details
    - Module list
    - Progress indicators

7. **Episode View**
    - Episode content
    - Grammar rules
    - Exercise sequence

8. **Grammar Reference**
    - Topic browsing
    - Grammar rule display
    - Examples and tables

### Priority 3 (Nice to Have)

9. **Study Session / Review**
    - Flashcard-style review
    - Rating interface

10. **Profile / Settings**
    - User stats
    - Settings options

### Out of Scope for Mockups

- Admin interface (separate project)
- Mobile-specific views (focus web first)
- Error states (focus happy path)
- Loading states (focus loaded content)

---

## Visual Style Direction

### Aesthetic Preferences

**Overall Feel:**

- Clean and modern
- Professional but approachable
- Spacious (not cramped)
- Calm color palette (not overly colorful)

**Reference Styles:**

- **Clean & Minimal:** Notion, Linear, Readwise
- **NOT:** Duolingo (too playful/gamified), traditional LMS platforms (too corporate)

**Key Visual Elements:**

1. **Typography**
    - Clear, readable fonts
    - Generous line height
    - Clear size hierarchy (h1, h2, body, small)

2. **Colors**
    - Soft, muted primary color (not bright)
    - Plenty of white/light backgrounds
    - Subtle grays for UI chrome
    - Green for success/completion
    - Red/orange for errors (used sparingly)
    - Accent color for interactive elements

3. **Cards & Containers**
    - Subtle shadows (not heavy drop shadows)
    - Rounded corners (modern)
    - Clear separation between elements

4. **Spacing**
    - Generous margins and padding
    - Visual breathing room
    - Clear grouping of related items

5. **Icons**
    - Simple, line-style icons
    - Consistent style across platform
    - Used purposefully, not decoratively

### Component Patterns to Consider

**Cards:**

- Content items (dialogues, stories)
- Task items
- Stat widgets

**Lists:**

- Vocabulary words
- Module/episode lists
- Grammar topics

**Progress Indicators:**

- Progress bars (courses, episodes)
- Completion checkmarks
- Streak counters

**Interactive Elements:**

- Buttons (primary, secondary, text)
- Input fields (search, text entry)
- Toggles (show/hide transcript)
- Dropdown filters

**Media:**

- Cover images
- Audio player (minimal default, expanded when playing)
- Illustrations

---

## Navigation Structure Decision

**To Be Determined in Mockups:**

Create mockups for BOTH navigation styles:

### Option A: Top Navigation Bar

- 5-6 main sections in horizontal nav
- Clean, modern
- More content space
- Example: Home | Library | Courses | Vocabulary | Profile

### Option B: Sidebar Navigation

- Vertical sidebar (always visible or collapsible)
- More room for navigation items
- Feels more app-like
- Can show sub-sections
- Example:
  ```
  Home
  Library
    ├─ Dialogues
    ├─ Stories
    └─ Grammar
  Courses
  Vocabulary
  Profile
  ```

**Decision Criteria:**

- Which feels more intuitive?
- Which provides better context?
- Which scales better with future features?

**Mockup Both** and compare.

---

## Interaction Patterns

### Common Interactions

**1. Saving Vocabulary**

- Click word in content → popup shows definition
- "Save to vocabulary" button in popup
- Feedback: "Saved!" confirmation
- Icon changes to indicate saved state

**2. Audio Playback**

- Minimalist by default: just "▶ Play Audio" button
- When playing:
    - Shows progress bar
    - Play/pause button
    - Time elapsed / total
    - Speed control (1x, 1.25x, 1.5x, 2x)
    - Transcript sync (optional: highlight current line)

**3. Exercise Submission**

- User selects/types answer
- "Submit" button enabled when answer provided
- On submit:
    - Immediate feedback (correct/incorrect)
    - Explanation shown
    - If correct: "Next (3s)" button with countdown
    - If incorrect: "Try Again" button
- Auto-advance after 3 seconds (if correct)

**4. Progress Tracking**

- Visual indicators throughout:
    - Checkmarks for completed items
    - Progress bars for courses/modules/episodes
    - Counters (e.g., "2/3 exercises")
- Progress saved automatically
- No manual "save progress" action needed

**5. Content Filtering**

- Dropdown filters (Level, Topic, etc.)
- Search bar
- Filter results update immediately
- Clear filter button if active
- Count of results shown

**6. Navigation**

- Breadcrumbs for deep paths (Course > Module > Episode)
- "Back" buttons where appropriate
- "Next" / "Previous" for sequences
- Clear "Home" or logo link to return

---

## Content Examples (For Mockup Use)

### Sample Dialogue

```
Title: At the Café
Type: Dialogue
Level: A1
Duration: 3 min
Topic: Food & Dining

Speakers: Thomas, Sophie, Waiter

Transcript:
Thomas: Bonjour! Une table pour deux, s'il vous plaît.
Waiter: Bien sûr. Par ici, s'il vous plaît.
Sophie: Merci beaucoup!
Waiter: Voici le menu. Je reviens dans un instant.
Thomas: Sophie, qu'est-ce que tu veux boire?
Sophie: Un café, s'il te plaît.

Vocabulary: table, s'il vous plaît, merci, menu, café
```

### Sample Story

```
Title: Sophie Arrives in Paris
Type: Story
Level: A1
Duration: 5 min

Text:
Sophie arrives at Charles de Gaulle airport in Paris. She is excited but also nervous. This is her first time in France.

She takes a taxi to her hotel in the center of Paris. The taxi driver is friendly. He asks, "C'est votre première fois à Paris?" Sophie smiles and says, "Oui!"

At the hotel, Sophie checks in. The receptionist gives her a key and says, "Bienvenue à Paris!" Sophie is very happy.

Vocabulary: airport, taxi, hotel, first time, key, welcome
```

### Sample Grammar Rule

```
Topic: Present Tense - Être (to be)
Category: Verbs > Present Tense
Level: A1

Explanation:
"Être" means "to be" in English. It is an irregular verb and one of the most important verbs in French.

Conjugation:
je suis       - I am
tu es         - you are (informal)
il/elle est   - he/she is
nous sommes   - we are
vous êtes     - you are (formal/plural)
ils/elles sont - they are

Examples:
- Je suis étudiant. (I am a student.)
- Elle est française. (She is French.)
- Nous sommes à Paris. (We are in Paris.)
```

### Sample Exercises

**Multiple Choice:**

```
Question: What is the French word for "thank you"?
Options:
A. Bonjour
B. Merci
C. Au revoir
D. S'il vous plaît

Correct: B
Explanation: "Merci" is the standard way to say thank you in French.
```

**Fill-in-Blank:**

```
Sentence: Je _____ au marché.
Correct Answer: vais
Hint: Use the verb "aller" (to go) in first person
Explanation: "Je vais" means "I go" or "I am going"
```

**Listening:**

```
Audio: [French phrase spoken]
Question: What did you hear?
Type: Multiple Choice
Options:
A. Bonjour
B. Bonsoir
C. Au revoir

Correct: A
```

---

## Non-Functional Requirements

### Performance

- Page load time: < 2 seconds
- Interaction response: < 100ms
- Audio start playback: < 1 second

### Accessibility

- WCAG 2.1 AA compliance
- Keyboard navigation for all functions
- Screen reader compatible
- Color contrast ratios meet standards

### Browser Support

- Chrome (latest 2 versions)
- Firefox (latest 2 versions)
- Safari (latest 2 versions)
- Edge (latest 2 versions)

### Data & Privacy

- User progress saved to backend
- Audio files served efficiently
- Secure authentication
- GDPR compliant (future consideration)

---

## Out of Scope (Not for Mockups)

1. **Mobile-specific designs** - Focus on web/desktop first
2. **Admin interfaces** - Separate project
3. **Social features** - Not implemented yet
4. **Payment/monetization** - Not current focus
5. **Error states** - Focus on happy path
6. **Loading states** - Assume content loaded
7. **Onboarding flow** - Can address later
8. **Settings detail** - Just placeholder
9. **Analytics dashboard** - Admin feature
10. **Multi-language UI** - Platform UI in English for now (content is in target language)

---

## Success Criteria for Mockups

Good mockups should demonstrate:

✅ **Clear information architecture** - User knows where they are, what they can do
✅ **Intuitive navigation** - Getting around is obvious
✅ **Content-focused design** - Learning content is prominent, UI recedes
✅ **Visual hierarchy** - Eye naturally flows to important elements
✅ **Consistent patterns** - Similar actions look and work similarly
✅ **Appropriate density** - Spacious but not empty, organized but not cramped
✅ **Modern aesthetic** - Feels current, professional, approachable

---

## Mockup Deliverable Request

Please create HTML mockups (with CSS) for the **Priority 1 screens**:

1. Home / Daily Tasks
2. Library - Content Browse
3. Content Player (Dialogue)
4. Exercise View (show multiple choice as example)
5. Vocabulary Management

**For each screen:**

- Create two versions: Top Nav and Sidebar Nav
- Use placeholder content from examples above
- Include realistic text, not lorem ipsum
- Show interactive states (hover, active, disabled) in CSS
- Use modern, clean styling
- Mobile responsive not required (desktop focus)
- Use semantic HTML
- Include comments explaining sections

**Style Guidelines:**

- Spacious, clean design (like Notion or Linear)
- Soft color palette (not bright/gamified)
- Readable typography (generous line-height)
- Subtle shadows and rounded corners
- Focus on content, minimal UI chrome
- Audio controls minimalist until playing

**Total:** 10 HTML files (5 screens × 2 nav styles)

---

## Questions to Consider During Design

1. **Navigation:** Top nav or sidebar? Which feels better?
2. **Daily Tasks:** Should it be the home screen, or one click away?
3. **Content Grid:** How many cards per row? What metadata to show?
4. **Audio Player:** How minimal can we go while still being clear?
5. **Exercise Feedback:** How prominent should correct/incorrect indicators be?
6. **Progress Indicators:** Where and how often should progress be shown?
7. **Actions:** What should be buttons vs links vs interactive text?
8. **Hierarchy:** What's primary, secondary, tertiary on each screen?

---

## Appendix: Platform Statistics (For Context)

**Current Content:**

- 1 complete course (French A1)
- 10 modules
- 24 episodes (stories and dialogues)
- 240+ exercises across 6 types
- Multi-speaker audio for all dialogues
- 4M+ words in vocabulary database (from Wiktionary)
- 12 languages supported in vocabulary (French focus)

**Technologies:**

- Web application
- AI-generated audio (Gemini TTS)
- PostgreSQL database
- RESTful API backend
- Modern JavaScript frontend

**User Capabilities:**

- Create account, authenticate
- Browse and search vocabulary
- Save personal word lists
- Practice with spaced repetition
- Complete exercises with instant feedback
- Track progress across activities
- Access content multiple ways

---

**Document End**

This requirements document should provide complete functional context for creating innovative, user-focused mockups
without being influenced by existing implementation details.
