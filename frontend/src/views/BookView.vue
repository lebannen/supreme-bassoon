<template>
  <div class="book-view-container">
    <h1>Book Reader Demo</h1>

    <BookComponent :pages="bookPages" />

    <div class="demo-section">
      <h2>How to Use</h2>
      <pre><code>&lt;BookComponent :pages="bookPages" /&gt;

// Or with raw content:
&lt;BookComponent :content="longText" :pageSize="300" /&gt;

// Or with custom slots:
&lt;BookComponent&gt;
  &lt;template #left-page="{ page }"&gt;
    Custom content for page {{ page }}
  &lt;/template&gt;
  &lt;template #right-page="{ page }"&gt;
    Custom content for page {{ page }}
  &lt;/template&gt;
&lt;/BookComponent&gt;</code></pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import BookComponent from '../components/BookComponent.vue'

const bookPages = [
  `Chapter 1: Introduction

Welcome to this beautiful book reader component! This component demonstrates a realistic page-flipping animation that brings your content to life.

The pages turn with a smooth 3D rotation effect, mimicking the experience of reading a physical book.

You can navigate using the Previous and Next buttons below the book.`,

  `Features

• Smooth 3D page-flip animation
• Responsive design for all screen sizes
• Customizable content via props or slots
• Page numbering
• Navigation controls
• Realistic book appearance with spine and shadows`,

  `Chapter 2: Content Options

You can provide content in three ways:

1. Array of pages - Each string becomes one page
2. Raw content string - Automatically split into pages
3. Custom slots - Full control over page rendering`,

  `Technical Details

Built with Vue 3 and TypeScript, this component uses:
• CSS 3D transforms for the flip effect
• Perspective for depth
• Backface-visibility for clean animations
• Transform-origin for realistic rotation`,

  `Chapter 3: Styling

The book uses a warm, paper-like background (#fffef0) and serif fonts for readability.

The spine creates a realistic center divider, and subtle shadows add depth to the pages.

All colors respect the PrimeVue theme variables for dark mode compatibility.`,

  `Responsive Design

The component adapts to different screen sizes:
• Desktop: 800x600px book
• Tablet: 600x450px book
• Mobile: 400x300px book

Controls stack vertically on small screens for better usability.`,

  `The End

Thank you for exploring this book component!

You can use it to display:
• Story content
• Documentation
• Vocabulary lessons
• Any paginated content

Happy reading!`
]
</script>

<style scoped>
.book-view-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
}

h1 {
  text-align: center;
  color: var(--text-color);
  font-weight: 700;
  margin-bottom: 2rem;
}

.demo-section {
  margin-top: 4rem;
  padding: 2rem;
  background: var(--surface-card);
  border-radius: var(--border-radius);
  border: 1px solid var(--surface-border);
}

.demo-section h2 {
  color: var(--text-color);
  margin-bottom: 1rem;
}

pre {
  background: var(--surface-ground);
  padding: 1.5rem;
  border-radius: 8px;
  overflow-x: auto;
}

code {
  font-family: 'Monaco', 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--text-color);
}
</style>
