#!/usr/bin/env python3
"""
Extract all Wiktionary entries for specified languages.

Parses the full Wiktionary XML dump and extracts entries for target languages,
saving them as compressed JSONL files (one per language).
"""

import sys
import re
import json
import gzip
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime
from typing import Dict, TextIO
from wikitext_parser import parse_entry


# Target languages to extract
TARGET_LANGUAGES = [
    'Spanish',
    'Italian',
    'Russian',
    'Portuguese',
    'French',
    'German',
    'Swedish',
    'Chinese',
    'Finnish',
    'Japanese',
    'Polish',
    'Dutch'
]


def extract_language_section(wikitext: str, language: str) -> str:
    """
    Extract a specific language section from the full wikitext.

    Args:
        wikitext: Full wikitext content
        language: Language name to extract

    Returns:
        Wikitext for that language section only
    """
    pattern = rf'^==\s*{re.escape(language)}\s*==\s*$(.*?)(?=^==\s*[^=]|\Z)'
    match = re.search(pattern, wikitext, re.MULTILINE | re.DOTALL)

    if match:
        return match.group(1).strip()

    return None


class LanguageExtractor:
    """
    Extracts entries for multiple languages from Wiktionary XML dump.
    """

    def __init__(self, xml_path: str, output_dir: str):
        self.xml_path = xml_path
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)

        # File handles for each language
        self.output_files: Dict[str, TextIO] = {}

        # Statistics
        self.stats = {
            'total_pages': 0,
            'pages_with_target_langs': 0,
            'entries_extracted': {lang: 0 for lang in TARGET_LANGUAGES},
            'errors': 0,
            'start_time': datetime.now()
        }

    def open_output_files(self):
        """Open compressed output files for each language."""
        for language in TARGET_LANGUAGES:
            filename = f'{language.lower()}.jsonl.gz'
            filepath = self.output_dir / filename

            self.output_files[language] = gzip.open(
                filepath,
                'wt',
                encoding='utf-8',
                compresslevel=9
            )

            print(f'📝 Opened output file: {filepath}')

    def close_output_files(self):
        """Close all output files."""
        for f in self.output_files.values():
            f.close()

    def write_entry(self, language: str, entry: dict):
        """Write a parsed entry to the appropriate language file."""
        if language in self.output_files:
            json_line = json.dumps(entry, ensure_ascii=False)
            self.output_files[language].write(json_line + '\n')
            self.stats['entries_extracted'][language] += 1

    def parse_page(self, page_elem):
        """
        Parse a single page element from the XML.

        Returns:
            Number of entries extracted from this page
        """
        # Extract page title and text
        title_elem = page_elem.find('./{*}title')
        text_elem = page_elem.find('.//{*}text')

        if title_elem is None or text_elem is None:
            return 0

        title = title_elem.text
        wikitext = text_elem.text

        if not title or not wikitext:
            return 0

        # Skip special pages
        if ':' in title:  # Skip pages like "Template:...", "Category:...", etc.
            return 0

        extracted_count = 0

        # Try to extract each target language
        for language in TARGET_LANGUAGES:
            try:
                lang_section = extract_language_section(wikitext, language)

                if lang_section:
                    # Parse the section (returns list of entries, one per POS)
                    parsed_entries = parse_entry(language, title, lang_section)

                    # Write each entry (one per POS section)
                    for parsed in parsed_entries:
                        self.write_entry(language, parsed)

                    if parsed_entries:
                        extracted_count += 1

            except Exception as e:
                self.stats['errors'] += 1
                if self.stats['errors'] <= 10:  # Only print first 10 errors
                    print(f'⚠️  Error parsing {title} ({language}): {e}')

        return extracted_count

    def extract(self):
        """
        Main extraction process using iterparse for memory efficiency.
        """
        print('='*70)
        print('Wiktionary Language Extraction')
        print('='*70)
        print(f'Source XML: {self.xml_path}')
        print(f'Output directory: {self.output_dir}')
        print(f'Target languages: {", ".join(TARGET_LANGUAGES)}')
        print('='*70)
        print()

        self.open_output_files()

        try:
            # Use iterparse to process XML incrementally
            context = ET.iterparse(self.xml_path, events=('end',))

            for event, elem in context:
                # Process page elements
                if elem.tag.endswith('page'):
                    self.stats['total_pages'] += 1

                    entries = self.parse_page(elem)

                    if entries > 0:
                        self.stats['pages_with_target_langs'] += 1

                    # Clear element to free memory
                    elem.clear()

                    # Progress update every 10,000 pages
                    if self.stats['total_pages'] % 10000 == 0:
                        self.print_progress()

            # Final progress
            print()
            self.print_final_stats()

        finally:
            self.close_output_files()

    def print_progress(self):
        """Print progress update."""
        elapsed = (datetime.now() - self.stats['start_time']).total_seconds()
        pages_per_sec = self.stats['total_pages'] / elapsed if elapsed > 0 else 0
        total_entries = sum(self.stats['entries_extracted'].values())

        print(f'\r📊 Pages: {self.stats["total_pages"]:,} | '
              f'Entries: {total_entries:,} | '
              f'Speed: {pages_per_sec:.0f} pages/sec | '
              f'Errors: {self.stats["errors"]}',
              end='', flush=True)

    def print_final_stats(self):
        """Print final statistics."""
        elapsed = (datetime.now() - self.stats['start_time']).total_seconds()
        total_entries = sum(self.stats['entries_extracted'].values())

        print()
        print('='*70)
        print('EXTRACTION COMPLETE')
        print('='*70)
        print(f'Total pages processed: {self.stats["total_pages"]:,}')
        print(f'Pages with target languages: {self.stats["pages_with_target_langs"]:,}')
        print(f'Total entries extracted: {total_entries:,}')
        print(f'Errors: {self.stats["errors"]}')
        print(f'Time elapsed: {elapsed/60:.1f} minutes')
        print(f'Average speed: {self.stats["total_pages"]/elapsed:.0f} pages/sec')
        print()
        print('Entries per language:')
        print('-'*70)

        # Sort by count descending
        sorted_langs = sorted(
            self.stats['entries_extracted'].items(),
            key=lambda x: x[1],
            reverse=True
        )

        for language, count in sorted_langs:
            filename = f'{language.lower()}.jsonl.gz'
            filepath = self.output_dir / filename

            # Get file size
            try:
                size_mb = filepath.stat().st_size / 1024 / 1024
                print(f'  {language:<15} {count:>8,} entries  ({size_mb:>6.1f} MB)')
            except:
                print(f'  {language:<15} {count:>8,} entries')


def main():
    """Main entry point."""
    xml_path = '/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml'
    output_dir = '/Users/andrii/Projects/vocabee/parsed_data'

    if not Path(xml_path).exists():
        print(f'❌ Error: XML file not found: {xml_path}')
        sys.exit(1)

    extractor = LanguageExtractor(xml_path, output_dir)

    try:
        extractor.extract()
    except KeyboardInterrupt:
        print('\n\n⚠️  Extraction interrupted by user')
        print('Closing files...')
        extractor.close_output_files()
        extractor.print_final_stats()
    except Exception as e:
        print(f'\n\n❌ Error: {e}')
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
