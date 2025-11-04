# Wiktionary Processing Scripts

This directory contains scripts for processing and querying the Wiktionary XML dump.

## Scripts Overview

### 1. `build_index.py`
**Purpose:** Build a fast lookup index for the Wiktionary XML dump.

**What it does:**
- Scans the entire XML dump (10GB+)
- Records byte offset for each `<page>` entry
- Creates SQLite database: `wiktionary_index.db`
- Enables instant lookup without parsing entire file

**Usage:**
```bash
python3 build_index.py
```

**Output:**
- `wiktionary_index.db` - SQLite database (~200-300 MB)
- Contains: word title → byte offset mapping

**Time:** ~10-12 minutes for full 10GB dump

---

### 2. `query_entry.py`
**Purpose:** Fast lookup tool for Wiktionary entries using the index.

**Features:**
- Instant lookup by exact word
- Search with wildcards
- Interactive mode
- Returns raw XML content

**Usage:**

**Direct lookup:**
```bash
python3 query_entry.py gratis
```

**Search with pattern:**
```bash
python3 query_entry.py --search "grat%"
```

**Interactive mode:**
```bash
python3 query_entry.py --interactive
```

Interactive commands:
- `<word>` - Look up exact word
- `/search <pattern>` - Search (use `%` as wildcard)
- `/stats` - Show statistics
- `/quit` - Exit

**Example:**
```bash
$ python3 query_entry.py gratis
Entry: gratis
Page ID: 41
Namespace: 0

======================================================================
XML CONTENT:
======================================================================

<page>
  <title>gratis</title>
  <ns>0</ns>
  <id>41</id>
  <text>
    ==English==
    ...
    ==Spanish==
    ...
  </text>
</page>
```

---

### 3. `parse_wiktionary.py`
**Purpose:** Analyze and count entries per language across the entire dump.

**What it does:**
- Parses all pages
- Counts language sections (==LanguageName==)
- Generates statistics report

**Usage:**
```bash
python3 parse_wiktionary.py
```

**Output:**
- Console report of top 50 languages
- `wiktionary_language_stats.txt` - Full statistics file

**Time:** ~3-5 minutes

---

### 4. `split_by_language.py`
**Purpose:** Split XML dump into separate files per language.

**What it does:**
- Extracts each language section from multi-language pages
- Outputs JSONL files (one per language)
- Each line = one entry with title, page_id, language, content

**Usage:**
```bash
python3 split_by_language.py
```

**Configuration:**
Edit the script to specify:
- `target_languages` - which languages to extract
- `max_pages` - limit for testing (None = all pages)

**Output:** `wiktionary_split/`
```
Spanish.jsonl      - Spanish entries
French.jsonl       - French entries
German.jsonl       - German entries
...
```

**Format (JSONL):**
```json
{"title": "gratis", "original_page_id": "41", "language": "Spanish", "content": "===Etymology===\n..."}
{"title": "libro", "original_page_id": "123", "language": "Spanish", "content": "===Etymology===\n..."}
```

**Time:**
- Test mode (100k pages): ~30 seconds
- Full dump: ~20-30 minutes

---

### 5. `extract_samples.py`
**Purpose:** Extract sample entries from specific languages for analysis.

**Usage:**
```bash
python3 extract_samples.py
```

**Output:** `wiktionary_samples.txt`

---

### 6. `analyze_structure.py`
**Purpose:** Deep-dive analysis of specific entry structure.

**Usage:**
```bash
python3 analyze_structure.py
```

---

## Workflow

### Initial Setup
```bash
# 1. Build the index (do this once)
python3 build_index.py

# 2. Get language statistics
python3 parse_wiktionary.py
```

### Quick Lookups
```bash
# Look up specific words
python3 query_entry.py hello
python3 query_entry.py --search "grat%"

# Interactive exploration
python3 query_entry.py --interactive
```

### Data Extraction
```bash
# Split into language-specific files
python3 split_by_language.py
```

## Index Database Schema

**Table: `page_index`**
| Column | Type | Description |
|--------|------|-------------|
| title | TEXT | Word/page title (primary key) |
| page_id | TEXT | Original Wiktionary page ID |
| namespace | TEXT | Namespace (0 = dictionary) |
| byte_offset | INTEGER | Position in XML file |
| byte_length | INTEGER | Size of entry in bytes |

**Table: `metadata`**
- Stores indexing metadata (path, timestamp, counts)

## Performance

**Index lookup:** < 1ms (SQLite query + seek)
**Full scan:** ~10-15 minutes
**Memory usage:** Minimal (streaming processing)

## Requirements

- Python 3.7+
- Standard library only (no external dependencies)
- ~300MB disk space for index
- 10GB+ for XML dump

## Notes

- **Case-sensitive:** Searches are case-sensitive
- **Exact matching:** Index uses exact title matching
- **Wildcards:** Use `%` in search patterns (SQL LIKE syntax)
- **Namespace 0:** Main dictionary entries (excludes meta pages)
