# Vocabee UI/UX Mockups

## Overview

This directory contains HTML mockups for the Vocabee platform redesign. Each mockup is a standalone HTML file with embedded CSS that demonstrates the design for Priority 1 screens.

**Created:** January 16, 2025
**Based on:** `/Users/andrii/Projects/vocabee/docs/REDESIGN_REQUIREMENTS.md`

---

## Files Included

### 1. Home / Daily Tasks
- **home-topnav.html** - Home page with top horizontal navigation
- **home-sidebar.html** - Home page with left sidebar navigation

**What it shows:**
- Personalized greeting and streak tracking
- Quick stats (study streak, words learned, time this week)
- Today's task list with completion status
- Recommended content cards

**Key features:**
- Task cards with checkboxes, metadata, and action buttons
- Visual progress indicators
- Clean stat cards with color-coded values
- Content recommendations in a grid layout

---

### 2. Library - Content Browse
- **library-topnav.html** - Library browse with top navigation
- **library-sidebar.html** - Library browse with sidebar navigation

**What it shows:**
- Tabbed interface for Dialogues, Stories, and Grammar
- Filter bar with search, level, topic, and sort options
- Grid of content cards with rich metadata
- Completion badges and audio indicators

**Key features:**
- Comprehensive filtering and search
- Visually rich content cards with gradient backgrounds
- Level badges and content type indicators
- Hover effects for interactive feedback
- Results count display

---

### 3. Content Player - Dialogue
- **content-player-topnav.html** - Dialogue player with top navigation
- **content-player-sidebar.html** - Dialogue player with sidebar navigation

**What it shows:**
- Breadcrumb navigation
- Content header with title, description, and metadata
- Minimalist audio player that expands when playing
- Interactive transcript with clickable words
- Key vocabulary list
- Action buttons (Mark Complete, Previous, Next)

**Key features:**
- Clean audio player with progress bar and speed control
- Toggle-able transcript visibility
- Word-level interactivity (hover effects)
- Vocabulary extraction and display
- Example word popup (shown but hidden by default)

---

### 4. Exercise View
- **exercise-topnav.html** - Exercise interface with top navigation
- **exercise-sidebar.html** - Exercise interface with sidebar navigation

**What it shows:**
- Exercise progress indicator
- Multiple choice exercise (as example)
- Question display with options
- Feedback section (correct answer shown)
- Hint functionality
- Auto-advance countdown

**Key features:**
- Progress bar showing completion
- Large, readable question text
- Clear option selection states
- Success/error feedback with explanations
- Auto-advance with countdown
- Skip and Submit/Next buttons

**Note:** HTML comments in the file show alternative states (incorrect answer, unattempted)

---

### 5. Vocabulary Management
- **vocabulary-topnav.html** - Vocabulary page with top navigation
- **vocabulary-sidebar.html** - Vocabulary page with sidebar navigation

**What it shows:**
- Study session cards (Due Reviews, Quick Review, All Words, Weak Words)
- Filter and search bar
- List of saved vocabulary words with metadata
- Status badges (Due, Learning, Mastered)
- Word actions (Review, Edit)

**Key features:**
- Multiple study session types with time estimates
- Highlighted "Due" session cards
- Comprehensive filtering (source, status, sort)
- Rich word metadata (source, date added, translation)
- Color-coded status indicators
- Clean list layout

---

## How to View

1. **Open in Browser:** Simply double-click any HTML file, or right-click → Open With → Browser
2. **No Server Needed:** All files are self-contained with embedded CSS
3. **Best Viewed:** In Chrome, Firefox, Safari, or Edge at desktop resolution (1200px+ width)

**Recommended viewing order:**
1. Home screens (to understand the dashboard)
2. Library screens (to see content browsing)
3. Content Player (to see how content is consumed)
4. Exercise screens (to see practice interface)
5. Vocabulary screens (to see study management)

---

## Design Approach

### Visual Style
- **Clean & Modern:** Inspired by Notion and Linear
- **Spacious:** Generous whitespace, breathing room between elements
- **Soft Colors:** Muted palette, no bright gamification
- **Content-First:** Minimal UI chrome, focus on learning content

### Color Palette
- **Primary:** `#6366f1` (Indigo) - Interactive elements, primary actions
- **Success:** `#10b981` (Green) - Completions, correct answers
- **Warning:** `#f59e0b` (Amber) - Due items, streaks
- **Error:** `#ef4444` (Red) - Incorrect answers (used sparingly)
- **Neutrals:** Grays from `#fafafa` (background) to `#1a1a1a` (text)

### Typography
- **Font Stack:** System fonts for native feel and performance
- **Sizes:** Clear hierarchy from 32px headers to 13px metadata
- **Line Height:** Generous 1.6 for body text, tighter for headings
- **Weights:** 700 for headers, 600 for emphasis, 500 for navigation

### Spacing & Layout
- **Max Width:** 1200px for reading content, 1400px for grids
- **Padding:** 48px standard page padding
- **Gaps:** 16-24px for cards, 32-48px for sections
- **Border Radius:** 12px for cards, 8px for smaller elements

### Interactive Elements
- **Hover States:** Subtle lift and shadow increase
- **Active States:** Slight scale reduction for tactile feedback
- **Transitions:** Smooth 0.2s for most interactions
- **Disabled States:** Reduced opacity and no pointer

---

## Key Design Decisions

### 1. Audio Player
**Decision:** Minimalist by default, expanded when playing
**Rationale:** Doesn't clutter the interface when not in use, clear controls when needed
**Implementation:** Simple play button + progress bar + speed control

### 2. Word Interaction
**Decision:** Clickable words in transcript, popup definition
**Rationale:** Inline learning without leaving context
**Implementation:** Hover effect on words, popup shown in HTML but hidden (demo purposes)

### 3. Task Cards
**Decision:** Horizontal layout with checkbox, info, and action button
**Rationale:** Scannable, clear hierarchy, obvious next action
**Implementation:** Flexbox layout, completed state with strikethrough

### 4. Content Cards
**Decision:** Vertical cards with image, badges, title, description, metadata
**Rationale:** Visually engaging, easy to scan, compact information
**Implementation:** Grid layout, gradient backgrounds for visual interest

### 5. Exercise Feedback
**Decision:** Immediate inline feedback with explanation
**Rationale:** Learn from mistakes in context, positive reinforcement
**Implementation:** Green/red bordered sections, clear icon indicators

### 6. Progress Indicators
**Decision:** Multiple types - bars, checkmarks, badges
**Rationale:** Different contexts need different visualizations
**Implementation:** Progress bars for sequences, checkmarks for tasks, badges for status

### 7. Filters & Search
**Decision:** Combined in a single sticky bar
**Rationale:** Always accessible, all controls in one place
**Implementation:** Flexbox row with search box and dropdowns

---

## Navigation Comparison: Top Nav vs Sidebar

### Top Navigation (Horizontal)

**Pros:**
- More horizontal space for content (no sidebar width loss)
- Modern, clean aesthetic
- Familiar pattern from many web apps
- Works well on wide screens

**Cons:**
- Limited space for navigation items (5-6 max)
- No room for sub-navigation without dropdowns
- Less contextual information visible

**Best for:**
- Minimalist approach
- Wide, content-heavy screens
- Users who prefer more screen real estate

---

### Sidebar Navigation (Vertical)

**Pros:**
- More navigation items and organization possible
- Can show sub-items (Library → Dialogues/Stories/Grammar)
- Persistent context (always visible)
- Room for user info at bottom
- Easier to add new sections

**Cons:**
- Takes up fixed horizontal space (260px)
- Reduces content area width
- Can feel more "app-like" than web-like

**Best for:**
- Complex navigation hierarchies
- Power users who navigate frequently
- Platforms with many sections

---

### Recommendation

**For Vocabee, I recommend the Sidebar Navigation** for these reasons:

1. **Better Organization:** Can show Library sub-items (Dialogues, Stories, Grammar) without extra clicks
2. **Scalability:** Easy to add new sections (e.g., Progress, Settings) without cluttering
3. **Context:** User info always visible at bottom, reinforces personal learning
4. **Consistency:** Many learning platforms (Duolingo, Memrise) use sidebars for good reason
5. **Efficiency:** Reduces clicks for power users who navigate frequently

The top nav version is cleaner and more minimal, but the sidebar provides better information architecture for a learning platform with multiple content types and features.

---

## Design Patterns Used

### Cards
Used for: Tasks, Content items, Study sessions, Vocabulary items
**Pattern:** White background, rounded corners, subtle shadow, hover lift

### Badges
Used for: Levels (A1, A2), Content types (Dialogue, Story), Status (Due, Learning)
**Pattern:** Colored background, rounded pill shape, uppercase small text

### Buttons
**Primary:** Indigo background, white text - main actions
**Secondary:** Light gray background, dark text - secondary actions
**Disabled:** Gray background, no interaction

### Lists
Used for: Vocabulary words, Grammar topics
**Pattern:** White container, items with borders, hover background change

### Grids
Used for: Content cards, Study sessions
**Pattern:** Auto-fill responsive grid, consistent gaps

---

## Interactive States Included

All mockups include CSS for:
- `:hover` - Mouse over feedback
- `:active` - Click feedback
- `.active` - Currently selected/active
- `.completed` - Finished tasks
- `.disabled` - Unavailable actions
- `.selected` - Chosen options
- `.correct` / `.incorrect` - Exercise feedback

---

## Responsive Design

**Note:** These mockups are optimized for desktop/tablet (1200px+ width). Mobile responsive design is out of scope per requirements, but could be added with:
- Collapsible sidebar (hamburger menu)
- Stacked layouts instead of grids
- Larger touch targets
- Simplified filter bars

---

## Missing Elements (Intentionally)

Per requirements, these mockups do NOT include:
- Loading states (assumed content is loaded)
- Error states (happy path only)
- Mobile layouts (desktop focus)
- Empty states (assumed content exists)
- Admin interfaces (separate project)
- Social features (not implemented yet)

---

## Next Steps

1. **Review mockups** with team/stakeholders
2. **Choose navigation pattern** (top nav vs sidebar)
3. **Refine based on feedback** (colors, spacing, interactions)
4. **Create component library** (reusable UI components)
5. **Implement in Vue.js** (translate HTML/CSS to components)
6. **Add interactivity** (real audio players, word popups, exercise logic)
7. **Test with users** (usability testing, iterations)

---

## Technical Notes

### CSS Architecture
- **Reset:** Minimal reset for consistent baseline
- **Variables:** Could extract colors and spacing to CSS variables
- **Organization:** Sections clearly commented with headers
- **Naming:** BEM-inspired (block__element--modifier style)

### Accessibility
- Semantic HTML5 elements (`nav`, `main`, `aside`, `section`)
- Sufficient color contrast ratios
- Clear visual hierarchy
- Keyboard-navigable (via standard HTML)
- Ready for ARIA labels and roles

### Browser Support
Tested in: Chrome, Firefox, Safari, Edge (latest versions)
Uses: Flexbox, Grid, CSS transitions (all well-supported)
No dependencies on external libraries

---

## Questions or Feedback?

If you have questions about design decisions, want alternative versions, or need modifications, please refer to the requirements document or reach out to the design team.

**Design Philosophy:** Less is more. Clear, clean, content-first.

---

**End of README**
