# French A1 Complete Course - Sophie's Parisian Journey

This is a complete French A1 beginner course following Sophie, an American IT professional, as she moves to Paris and navigates her new life in France.

## Course Structure

**Main Character:** Sophie Martin - An American woman from New York who moves to Paris for a job at TechParis

**Supporting Characters:**
- **Thomas Dubois** (Voice: Achird) - Sophie's friendly colleague from Lyon
- **Madame Laurent** (Voice: Gacrux) - Sophie's kind landlady
- **Lisa** (Voice: Laomedeia) - Sophie's American friend who visits

## Modules Overview

### Module 1: Sophie's First Day in Paris (Greetings & Introductions)
- Episode 1: Arrival at Charles de Gaulle Airport
- Episode 2: Checking Into the Hotel
- Episode 3: Meeting Thomas, the Colleague

**Learning Points:** Basic greetings, introductions, nationalities, professions

### Module 2: Sophie Finds a Home (Housing)
- Episode 1: Visiting an Apartment
- Episode 2: Meeting Madame Laurent, the Landlady
- Episode 3: Sophie's First Night

**Learning Points:** Room names, furniture, housing vocabulary, days of the week

### Module 3: Sophie's First Meals Out (At the Restaurant)
- Episode 1: Lunch with Thomas at a Café
- Episode 2: Dinner at a Traditional French Restaurant

**Learning Points:** Ordering food and drinks, menu vocabulary, asking for the bill

### Module 4: Sophie Goes Shopping (Shopping & Numbers)
- Episode 1: At the Clothing Store
- Episode 2: At the Grocery Store

**Learning Points:** Shopping vocabulary, colors, sizes, numbers, prices

### Module 5: Getting Around Paris (Directions & Transportation)
Sophie learns to navigate the Paris metro and ask for directions

### Module 6: Sophie's Daily Routine (Daily Life)
Following Sophie through a typical workday in Paris

### Module 7: Weekend Activities (Hobbies & Free Time)
Sophie explores Paris on weekends and talks about her hobbies

### Module 8: Meeting Thomas's Family (Family & Friends)
Sophie is invited to meet Thomas's family

### Module 9: Planning a Trip (Making Plans)
Sophie and Thomas plan a weekend trip to the French countryside

### Module 10: The Seasons in Paris (Weather & Seasons)
Sophie experiences all four seasons in her first year in Paris

## Features

- **Consistent Character Voices:**
  - Sophie: Puck (Upbeat, Middle pitch)
  - Thomas: Achird (Friendly, Lower middle pitch)
  - Madame Laurent: Gacrux (Mature, Middle pitch)
  - Various other characters with distinct voices

- **Exercise Types Per Episode:** Minimum 8 exercises including:
  - Multiple Choice (comprehension and vocabulary)
  - Fill in the Blank (single and multi-blank)
  - Sentence Scramble
  - Matching (vocabulary, phrases, concepts)
  - Cloze Reading
  - Listening (with audio generation)

- **Progressive Difficulty:** Episodes build on previous knowledge
- **Practical Focus:** Real-life situations Sophie encounters
- **Cultural Context:** French customs and culture integrated throughout

## Import Instructions

1. Import the course metadata:
```bash
curl -X POST http://localhost:8080/api/admin/courses/import \
  -H "Content-Type: application/json" \
  -d @course.json
```

2. Import each module (with audio generation):
```bash
curl -X POST "http://localhost:8080/api/admin/courses/french-a1-complete/modules/import?generateAudio=true" \
  -H "Content-Type: application/json" \
  -d @module_1.json

# Repeat for modules 2-10
```

## Learning Outcomes

By completing this course, students will be able to:
- Introduce themselves and have basic conversations
- Handle practical situations: hotels, restaurants, shopping, transportation
- Ask for and give simple directions
- Talk about daily routines and activities
- Describe family, friends, and relationships
- Make plans and appointments
- Discuss weather and preferences
- Use numbers for prices, dates, and time
- Express basic needs and opinions

Total estimated time: 45 hours
CEFR Level: A1

---

Created with attention to A1-level learners following CEFR guidelines.
