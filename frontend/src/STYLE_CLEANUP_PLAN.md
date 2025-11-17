# Style Cleanup Plan - Vocabee Frontend

## Executive Summary

Analysis of the codebase shows a **well-designed utility system** is in place, but many components still have custom
styles that could be replaced with existing utilities. This plan identifies opportunities to reduce per-component
styling by ~60-70%.

## Available Design System

### CSS Variables (theme.css)

- ✅ Complete color system (primary, backgrounds, text, borders, status)
- ✅ Spacing scale (--spacing-xs through --spacing-4xl)
- ✅ Border radius (--radius-sm through --radius-full)
- ✅ Shadows (--shadow-sm, --shadow-md, --shadow-lg)
- ✅ Dark mode support

### Utility Classes (utilities.css)

- ✅ Typography (.text-sm, .text-lg, .font-bold, etc.)
- ✅ Spacing (.gap-md, .mb-xl, etc.)
- ✅ Flexbox (.flex, .items-center, .justify-between, etc.)
- ✅ Grids (.content-grid, .stats-grid, .task-list)
- ✅ Page structure (.page-header, .section-header, .detail-header)
- ✅ States (.loading-state, .empty-state)
- ✅ Patterns (.icon-label-group, .meta-badges, .stats, etc.)
- ✅ Reading patterns (.view-header, .text-header, .filters)
- ✅ File upload patterns (.file-item, .upload-header)

### Component Styles (components.css)

- ✅ Card system (.card, .card-padding, .card-hoverable)
- ✅ Badge system (.badge, .badge-success, .badge-pill)
- ✅ Button system (.btn, .btn-primary, .btn-lg)

## Common Anti-Patterns Found

### 1. Repeated Container Styles (26 instances)

```css
/* ❌ BEFORE - Repeated in every view */
.some-container {
  min-height: 100vh;
  background: var(--surface-ground);
}

/* ✅ AFTER - Add to utilities.css once */
.page-container {
  min-height: 100vh;
  background: var(--surface-ground);
}
```

### 2. Custom Grid Layouts (duplicated ~15 times)

```css
/* ❌ BEFORE - Custom in each component */
.custom-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-lg);
}

/* ✅ AFTER - Already exists in utilities.css */
/* Use: class="content-grid" */
```

### 3. Repeated Stat/Card Patterns (duplicated ~10 times)

```css
/* ❌ BEFORE - Custom stat styles */
.my-stat {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

/* ✅ AFTER - Already exists in utilities.css */
/* Use: class="stat" */
```

### 4. Custom Empty/Loading States (8 instances)

```css
/* ❌ BEFORE - Duplicated in components */
.my-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rem;
  gap: 1rem;
}

/* ✅ AFTER - Already exists */
/* Use: class="loading-state" or class="empty-state" */
```

## Cleanup Priorities

### Phase 1: Add Missing Utilities (15 mins)

Add these commonly needed but missing utilities to `utilities.css`:

```css
/* Page containers - used in 26+ views */
.page-container {
  min-height: 100vh;
  background: var(--surface-ground);
}

.page-container-with-padding {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: var(--spacing-2xl);
}

/* Icon colors - used everywhere */
.icon-primary { color: var(--primary); }
.icon-success { color: var(--success); }
.icon-warning { color: var(--warning); }
.icon-error { color: var(--error); }

/* Stat icon wrapper - used in stats, dashboard */
.stat-icon {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  flex-shrink: 0;
}

/* Colored stat icon variants */
.stat-icon-warning { background: var(--warning-light); color: var(--warning); }
.stat-icon-success { background: var(--success-light); color: var(--success); }
.stat-icon-info { background: var(--info-light); color: var(--info); }
.stat-icon-purple { background: #f3e8ff; color: #8b5cf6; }

/* Hoverable cards - used throughout */
.card-interactive {
  cursor: pointer;
  transition: all 0.2s;
}

.card-interactive:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* Activity/List items */
.list-item-interactive {
  padding: 1rem;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-secondary);
}

.list-item-interactive:hover {
  border-color: var(--primary);
  background: var(--primary-light);
  transform: translateX(4px);
}

/* Gradient backgrounds */
.gradient-warning {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
}

.gradient-purple {
  background: linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%);
}

/* Uppercase labels */
.text-uppercase {
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Dividers */
.divider-top {
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-medium);
}

.divider-bottom {
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-medium);
}
```

### Phase 2: Refactor High-Impact Views (1-2 hours)

#### StatsView.vue

**Before:** 167 lines of custom CSS
**After:** ~20 lines (87% reduction)

Changes:

- `.stats-page-container` → `.page-container`
- `.icon-primary` → NEW utility
- `.stat-icon-wrapper` → NEW utility `.stat-icon`
- `.overview-stat` → existing `.stat` + modifier classes
- `.streaks-grid` → existing `.stats-grid`
- `.activity-item` → NEW utility `.list-item-interactive`
- `.language-stats` → existing `.summary-stats`
- `.modules-grid` → existing `.content-grid`

#### ExercisesView.vue

Custom grids → existing `.content-grid`

#### WordSetsView.vue

Custom grids → existing `.content-grid`

#### CoursesView.vue

Custom grids → existing `.content-grid`

### Phase 3: Standardize All Views (2-3 hours)

Refactor remaining 20+ views to use utilities:

- Replace custom containers with `.page-container`
- Replace custom grids with `.content-grid` / `.stats-grid`
- Replace custom headers with `.page-header` / `.section-header`
- Replace custom loading states with `.loading-state`
- Replace custom empty states with `.empty-state`

## Estimated Impact

### Before

- **Total custom CSS:** ~2,500 lines across all components/views
- **Duplication rate:** ~45% (same patterns repeated)
- **Maintenance burden:** High (changes need to be made in multiple places)

### After

- **Total custom CSS:** ~800 lines (only truly unique styles)
- **Duplication rate:** ~5% (minimal repetition)
- **New utilities added:** ~100 lines (covers 80% of common patterns)
- **Net reduction:** ~1,600 lines removed
- **Maintenance burden:** Low (change once in utilities.css)

## Benefits

1. **Consistency:** All stats, cards, grids look the same
2. **Maintainability:** Change design system once, affects all components
3. **Faster development:** Copy existing patterns instead of writing CSS
4. **Smaller bundles:** Less CSS to parse and download
5. **Easier onboarding:** Developers learn utility classes once
6. **Dark mode:** Utilities use CSS variables, so dark mode works automatically

## Recommended Approach

1. **Week 1:** Add new utilities (Phase 1)
2. **Week 2:** Refactor 5 high-impact views (Phase 2)
3. **Week 3:** Refactor remaining views systematically (Phase 3)
4. **Ongoing:** New components use utilities by default

## Guidelines for Future Components

### ✅ DO

- Use existing utility classes first
- Use CSS variables for colors, spacing, shadows
- Add new utilities if pattern appears 3+ times
- Keep component CSS under 50 lines
- Focus component CSS on truly unique styling

### ❌ DON'T

- Create custom grids (use `.content-grid`, `.stats-grid`)
- Create custom containers (use `.page-container`)
- Create custom loading/empty states (use utilities)
- Hard-code colors, spacing, or shadows
- Duplicate existing utility patterns

## Example Refactor

### Before

```vue
<template>
  <div class="my-view-container">
    <div class="my-header">
      <h1>My Page</h1>
      <p>Description here</p>
    </div>
    <div class="my-grid">
      <div class="my-card" v-for="item in items">...</div>
    </div>
  </div>
</template>

<style scoped>
.my-view-container {
  min-height: 100vh;
  background: var(--surface-ground);
  padding: var(--spacing-2xl);
}

.my-header {
  margin-bottom: var(--spacing-3xl);
}

.my-header h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 0.375rem;
  color: var(--text-primary);
}

.my-header p {
  font-size: 1rem;
  color: var(--text-secondary);
}

.my-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-lg);
}

.my-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  cursor: pointer;
  transition: all 0.2s;
}

.my-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
</style>
```

### After

```vue
<template>
  <div class="page-container-with-padding">
    <div class="page-header">
      <h1>My Page</h1>
      <p>Description here</p>
    </div>
    <div class="content-grid">
      <Card v-for="item in items" class="card-hoverable">...</Card>
    </div>
  </div>
</template>

<style scoped>
/* No custom CSS needed! */
</style>
```

## Next Steps

1. Review and approve this plan
2. Add new utilities to `utilities.css`
3. Start refactoring views, highest-impact first
4. Update component documentation with utility examples
