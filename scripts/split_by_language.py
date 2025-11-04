#!/usr/bin/env python3
"""
Split Wiktionary XML dump into language-specific files.

Strategy:
- Parse each <page> element
- Extract each language section from the wikitext
- Create separate output entries for each language
- Maintain original metadata
"""

import xml.etree.ElementTree as ET
import re
import os
from collections import defaultdict
from datetime import datetime

class LanguageSplitter:
    def __init__(self, dump_path, output_dir, target_languages=None):
        self.dump_path = dump_path
        self.output_dir = output_dir
        self.target_languages = set(target_languages) if target_languages else None

        # Statistics
        self.stats = defaultdict(int)
        self.entries_per_language = defaultdict(int)

        # Create output directory
        os.makedirs(output_dir, exist_ok=True)

        # File handles for each language (we'll write as we go)
        self.language_files = {}

    def split_by_language_sections(self, wikitext):
        """
        Split wikitext into language sections.
        Returns: [(language_name, content), ...]
        """
        if not wikitext:
            return []

        results = []

        # Pattern to match language sections
        # Matches: ==LanguageName==  and captures everything until next ==AnotherLanguage== or end
        pattern = r'^==([^=\n]+)==\s*$(.+?)(?=^==[^=\n]+==$|\Z)'
        matches = re.finditer(pattern, wikitext, re.MULTILINE | re.DOTALL)

        for match in matches:
            lang = match.group(1).strip()
            content = match.group(2).strip()
            results.append((lang, content))

        return results

    def get_language_file(self, language):
        """
        Get or create file handle for a specific language.
        """
        if language not in self.language_files:
            # Sanitize language name for filename
            safe_name = re.sub(r'[^\w\s-]', '', language).strip().replace(' ', '_')
            filepath = os.path.join(self.output_dir, f"{safe_name}.jsonl")
            self.language_files[language] = open(filepath, 'w', encoding='utf-8')

        return self.language_files[language]

    def write_entry(self, language, title, page_id, content):
        """
        Write an entry to the language-specific file.
        Using JSONL format for easy processing later.
        """
        import json

        entry = {
            'title': title,
            'original_page_id': page_id,
            'language': language,
            'content': content
        }

        f = self.get_language_file(language)
        f.write(json.dumps(entry, ensure_ascii=False) + '\n')

        self.entries_per_language[language] += 1

    def process_dump(self, max_pages=None):
        """
        Process the entire dump and split by language.
        max_pages: for testing, limit number of pages to process
        """
        print(f"Processing dump: {self.dump_path}")
        print(f"Output directory: {self.output_dir}")
        if self.target_languages:
            print(f"Target languages: {', '.join(sorted(self.target_languages))}")
        else:
            print("Processing ALL languages")
        print(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

        last_report = datetime.now()

        try:
            context = ET.iterparse(self.dump_path, events=('end',))

            for event, elem in context:
                if elem.tag.endswith('page'):
                    self.stats['total_pages'] += 1

                    # Find namespace
                    ns_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}ns')
                    if ns_elem is not None and ns_elem.text == '0':
                        self.stats['main_namespace'] += 1

                        # Extract metadata
                        title_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}title')
                        id_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}id')
                        text_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}text')

                        if title_elem is not None and text_elem is not None and text_elem.text:
                            title = title_elem.text
                            page_id = id_elem.text if id_elem is not None else 'unknown'

                            # Split into language sections
                            lang_sections = self.split_by_language_sections(text_elem.text)

                            if lang_sections:
                                self.stats['pages_with_languages'] += 1

                                for lang, content in lang_sections:
                                    # Filter by target languages if specified
                                    if self.target_languages is None or lang in self.target_languages:
                                        self.write_entry(lang, title, page_id, content)

                    # Progress reporting
                    now = datetime.now()
                    if (now - last_report).seconds >= 10:
                        print(f"Progress: {self.stats['total_pages']:,} pages, "
                              f"{self.stats['main_namespace']:,} entries, "
                              f"{len(self.entries_per_language)} languages, "
                              f"{sum(self.entries_per_language.values()):,} language entries extracted")
                        last_report = now

                    # Clear element
                    elem.clear()

                    # Stop if max_pages reached
                    if max_pages and self.stats['total_pages'] >= max_pages:
                        print(f"\nReached max_pages limit: {max_pages}")
                        break

        except KeyboardInterrupt:
            print("\n\nProcessing interrupted by user.")
        except Exception as e:
            print(f"\n\nError: {e}")
            import traceback
            traceback.print_exc()
        finally:
            # Close all file handles
            for f in self.language_files.values():
                f.close()

        self.print_summary()

    def print_summary(self):
        """Print processing summary."""
        print("\n" + "="*70)
        print("PROCESSING COMPLETE")
        print("="*70)
        print(f"Total pages processed: {self.stats['total_pages']:,}")
        print(f"Dictionary entries: {self.stats['main_namespace']:,}")
        print(f"Pages with languages: {self.stats['pages_with_languages']:,}")
        print(f"Languages extracted: {len(self.entries_per_language)}")
        print(f"Total language entries: {sum(self.entries_per_language.values()):,}")

        print("\n" + "="*70)
        print("TOP 30 LANGUAGES BY ENTRY COUNT")
        print("="*70)

        sorted_langs = sorted(self.entries_per_language.items(),
                             key=lambda x: x[1], reverse=True)

        for i, (lang, count) in enumerate(sorted_langs[:30], 1):
            print(f"{i:2}. {lang:30} {count:,} entries")

        print(f"\nOutput files saved to: {self.output_dir}/")


if __name__ == "__main__":
    import sys

    dump_path = "/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml"
    output_dir = "/Users/andrii/Projects/vocabee/wiktionary_split"

    # For testing: extract only top languages
    target_languages = [
        "English",
        "Spanish",
        "French",
        "German",
        "Italian",
        "Portuguese",
        "Russian",
        "Japanese",
        "Chinese"
    ]

    # Quick test with 100k pages
    print("="*70)
    print("WIKTIONARY LANGUAGE SPLITTER")
    print("="*70)
    print("\nRunning TEST MODE: First 100,000 pages only")
    print("This will take ~30 seconds\n")

    splitter = LanguageSplitter(dump_path, output_dir, target_languages)
    splitter.process_dump(max_pages=100000)
