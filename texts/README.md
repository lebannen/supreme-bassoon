# Reading Texts Directory

This directory contains JSON files with reading texts for language learners.

## Quick Start

1. **Review the plan**: See `../docs/reading-texts-plan.md` for full implementation details
2. **Use AI prompts**: See `AI_PROMPTS.md` for ready-to-use prompts
3. **Follow template**: Use `TEMPLATE.json` for formatting your texts
4. **See example**: Check `fr_a1_example.json` for a complete example

## File Naming Convention

```
{language}_{level}_{topic}.json

Examples:
- fr_a1_cafe.json
- de_a1_family.json
- fr_a2_travel.json
```

## Levels

- **A1**: Beginner (150-250 words)
- **A2**: Elementary (250-400 words)
- **B1**: Intermediate (400-600 words)
- **B2**: Upper Intermediate (600-800 words)

## Topics

Common topics by level:

**A1**: daily_life, food, family, shopping, greetings
**A2**: work, hobbies, health, travel, environment
**B1**: education, technology, culture, society
**B2**: science, politics, arts, philosophy

## Creating New Texts

### Option 1: AI Generation (Recommended)

1. Open `AI_PROMPTS.md`
2. Choose appropriate level and topic
3. Copy prompt to ChatGPT/Claude
4. Review and adjust output
5. Format using `TEMPLATE.json`
6. Save with proper filename

### Option 2: Manual Writing

1. Write text at appropriate level
2. Check vocabulary and grammar complexity
3. Format using `TEMPLATE.json`
4. Save with proper filename

## JSON Structure

```json
{
  "title": "Text title",
  "language_code": "fr|de|es|it|...",
  "level": "A1|A2|B1|B2|C1|C2",
  "topic": "daily_life|food|travel|...",
  "description": "Brief summary",
  "author": "ai|manual|username",
  "source": "ai-generated|manual|import",
  "content": "The actual text content...\n\nWith paragraphs."
}
```

## Quality Checklist

- [ ] Appropriate word count for level
- [ ] Vocabulary matches level
- [ ] Grammar structures match level
- [ ] Engaging and natural content
- [ ] Proper formatting (paragraphs, dialogue)
- [ ] No spelling/grammar errors
- [ ] Metadata filled correctly
- [ ] Filename follows convention

## Importing to Vocabee

Once the backend import system is implemented, texts will be imported via:
- API endpoint: `POST /api/reading/texts/import`
- Bulk import script (TBD)

## Current Status

**Phase**: Planning / Content Creation
**Next**: Backend implementation (see plan document)

---

**Last Updated**: 2025-01-09
