# AI Text Generation Prompts

Use these prompts with ChatGPT, Claude, or other AI models to generate reading texts for Vocabee.

---

## French A1 Level

### General Template

```
Generate a short reading text for French A1 learners.

Topic: [CHOOSE: daily_life / travel / food / culture / shopping / family]
Length: 150-250 words
Grammar: Simple present tense, basic past tense (passé composé)
Vocabulary: Common A1 words only

Requirements:
- Natural, engaging scenario
- Include dialogue if appropriate
- Use simple sentence structures
- Include common useful phrases
- Avoid complex grammar

Output plain text only with paragraphs (separated by blank lines).
Do NOT include title, metadata, or English translations.
```

### Specific Example Prompts

#### At a Bakery
```
Generate a short reading text for French A1 learners about visiting a bakery (boulangerie).

Length: 150-200 words
Include:
- Polite greetings (Bonjour, s'il vous plaît, merci)
- Common bakery items (pain, baguette, croissant, gâteau)
- Simple dialogue between customer and baker
- Prices if appropriate (euros)
- Present tense verbs

Output plain text with paragraphs and dialogue.
```

#### Daily Routine
```
Generate a French A1 reading text about someone's daily morning routine.

Length: 150-200 words
Include:
- Time expressions (le matin, à 7 heures, etc.)
- Daily activities (se réveiller, prendre le petit-déjeuner, aller au travail)
- First person narration
- Simple present tense
- Common household vocabulary

Output plain text with paragraphs.
```

#### At a Restaurant
```
Generate a French A1 reading text about eating at a restaurant.

Length: 150-200 words
Include:
- Restaurant vocabulary (menu, serveur, addition)
- Food and drink items
- Dialogue between customer and waiter
- Ordering phrases (Je voudrais..., L'addition s'il vous plaît)
- Simple present tense

Output plain text with paragraphs and dialogue.
```

#### Family Description
```
Generate a French A1 reading text where someone describes their family.

Length: 150-200 words
Include:
- Family member vocabulary (père, mère, frère, sœur, etc.)
- Age expressions (Il a ... ans)
- Occupation vocabulary
- Physical descriptions (grand, petit, cheveux)
- Possessive adjectives (mon, ma, mes)

Output plain text with paragraphs.
```

#### Weekend Activities
```
Generate a French A1 reading text about weekend activities.

Length: 150-200 words
Include:
- Day/time vocabulary (samedi, dimanche, le weekend)
- Common activities (faire du sport, regarder un film, rencontrer des amis)
- Near future tense (aller + infinitif)
- Place vocabulary (parc, cinéma, maison)

Output plain text with paragraphs.
```

---

## French A2 Level

### General Template

```
Generate a reading text for French A2 learners.

Topic: [CHOOSE: work / education / hobbies / health / environment / technology]
Length: 250-400 words
Grammar: Present, passé composé, imparfait, near future
Vocabulary: A1 + A2 level words

Requirements:
- More complex narrative or situation
- Can include opinions or comparisons
- Use relative pronouns (qui, que)
- Include some descriptive language
- Slightly longer sentences

Output plain text only with paragraphs.
```

---

## German A1 Level

### General Template

```
Generate a short reading text for German A1 learners.

Topic: [CHOOSE: daily_life / travel / food / family / shopping]
Length: 150-250 words
Grammar: Present tense, basic modal verbs (können, müssen, wollen)
Vocabulary: Common A1 words only

Requirements:
- Natural, engaging scenario
- Include dialogue if appropriate
- Use simple sentence structures
- Show correct article usage (der/die/das)
- Include common useful phrases
- Basic word order (verb second position)

Output plain text only with paragraphs.
Do NOT include title, metadata, or English translations.
```

### Specific Example Prompts

#### At a Café
```
Generate a German A1 reading text about visiting a café.

Length: 150-200 words
Include:
- Greetings (Guten Tag, Bitte, Danke)
- Café vocabulary (Kaffee, Kuchen, Tee)
- Simple dialogue
- Prices in euros
- Present tense
- Correct articles

Output plain text with paragraphs and dialogue.
```

#### Daily Routine
```
Generate a German A1 reading text about someone's daily routine.

Length: 150-200 words
Include:
- Time expressions (um 7 Uhr, morgens, abends)
- Separable verbs (aufstehen, einkaufen)
- Daily activities
- Present tense
- First person narration

Output plain text with paragraphs.
```

---

## Topics by Level

### A1 Topics (Beginner)
- Introductions and greetings
- Family and friends
- Daily routines
- Food and drink
- Shopping (basic)
- Directions (very simple)
- Numbers, time, dates
- Weather

### A2 Topics (Elementary)
- Work and studies
- Hobbies and free time
- Health and body
- Travel and transportation
- Past experiences
- Future plans
- Opinions (simple)
- Descriptions (people, places)

---

## After Generation Checklist

1. ✅ Check word count (150-250 for A1, 250-400 for A2)
2. ✅ Verify vocabulary level is appropriate
3. ✅ Ensure grammar structures match level
4. ✅ Check for natural, engaging content
5. ✅ Verify dialogue formatting if present
6. ✅ No English text included
7. ✅ Proper paragraph breaks
8. ✅ Save in JSON format using TEMPLATE.json

---

## JSON Format Example

```json
{
  "title": "Your Title Here",
  "language_code": "fr",
  "level": "A1",
  "topic": "daily_life",
  "description": "Brief description",
  "author": "ai",
  "source": "ai-generated",
  "content": "Generated text here...\n\nWith paragraph breaks.\n\n\"And dialogue.\""
}
```

---

## Recommended Generation Workflow

1. Choose language and level
2. Select topic from appropriate list
3. Use specific prompt or customize general template
4. Generate with AI (ChatGPT, Claude, etc.)
5. Review output for quality and level appropriateness
6. Format as JSON using template
7. Save with descriptive filename (e.g., `fr_a1_cafe.json`)
8. Import into Vocabee

---

## Quality Standards

**Good A1 Text Characteristics:**
- ✅ 150-250 words
- ✅ Simple, clear sentences
- ✅ Common, useful vocabulary
- ✅ Present tense primarily
- ✅ Natural dialogue or narrative
- ✅ Relevant to everyday situations
- ✅ Engaging and relatable

**Avoid in A1:**
- ❌ Complex subordinate clauses
- ❌ Subjunctive mood
- ❌ Advanced vocabulary
- ❌ Literary or formal language
- ❌ Abstract concepts
- ❌ Very long sentences
