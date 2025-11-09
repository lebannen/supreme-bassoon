#!/usr/bin/env python3
"""
Generate JSONL files for supported languages from Wiktionary XML dump.

This script:
1. Parses the entire Wiktionary XML dump
2. Extracts entries for the 12 supported languages
3. Parses each entry with WikitextParser
4. Writes compressed JSONL files with metadata
"""

import xml.etree.ElementTree as ET
import json
import gzip
import re
import os
import sys
from datetime import datetime
from collections import defaultdict
from pathlib import Path

# Add current directory to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from wikitext_parser import WikitextParser


class LanguageJsonlGenerator:
    def __init__(self, xml_path: str, output_dir: str):
        self.xml_path = xml_path
        self.output_dir = output_dir

        # Statistics
        self.stats = {
            'total_pages': 0,
            'main_namespace': 0,
            'pages_with_target_languages': 0,
            'entries_per_language': defaultdict(int),
            'errors_per_language': defaultdict(int)
        }

        # Create output directory
        os.makedirs(output_dir, exist_ok=True)

        # File handles for each language
        self.language_files = {}

        # Track if metadata has been written
        self.metadata_written = set()

    def get_language_file(self, language: str) -> gzip.GzipFile:
        """Get or create gzip file handle for a language."""
        if language not in self.language_files:
            language_code = WikitextParser.get_language_code(language)
            filepath = os.path.join(self.output_dir, f"{language_code}.jsonl.gz")

            self.language_files[language] = gzip.open(
                filepath, 'wt', encoding='utf-8', compresslevel=9
            )

            # Write metadata as first line
            metadata = {
                "_metadata": True,
                "language": language,
                "language_code": language_code,
                "parser_version": "0.2.0",
                "generated_at": datetime.now().isoformat()
            }
            self.language_files[language].write(json.dumps(metadata, ensure_ascii=False) + '\n')
            self.metadata_written.add(language)

            print(f"Created output file: {filepath}")

        return self.language_files[language]

    def extract_language_section(self, wikitext: str, target_language: str) -> str | None:
        """Extract a specific language section from wikitext."""
        if not wikitext:
            return None

        # Pattern to match language sections
        # Matches: ==LanguageName==  and captures everything until next ==AnotherLanguage== or end
        pattern = rf'^==\s*{re.escape(target_language)}\s*==\s*$(.+?)(?=^==\s*[^=\n]+\s*==\s*$|\Z)'
        match = re.search(pattern, wikitext, re.MULTILINE | re.DOTALL)

        if match:
            return match.group(1).strip()

        return None

    def process_entry(self, title: str, wikitext: str) -> None:
        """Process a single entry for all supported languages."""
        for language in WikitextParser.SUPPORTED_LANGUAGES.keys():
            lang_section = self.extract_language_section(wikitext, language)

            if not lang_section:
                continue

            try:
                # Parse the language section
                parser = WikitextParser(language, title, lang_section)
                entries = parser.parse()

                if not entries:
                    continue

                # Write each parsed entry
                file_handle = self.get_language_file(language)
                for entry in entries:
                    file_handle.write(json.dumps(entry, ensure_ascii=False) + '\n')
                    self.stats['entries_per_language'][language] += 1

            except Exception as e:
                self.stats['errors_per_language'][language] += 1
                if self.stats['errors_per_language'][language] <= 10:
                    print(f"Error parsing {title} ({language}): {e}")

    def process_dump(self, max_pages: int | None = None) -> None:
        """Process the entire XML dump."""
        print("="*70)
        print("WIKTIONARY JSONL GENERATOR")
        print("="*70)
        print(f"XML dump: {self.xml_path}")
        print(f"Output directory: {self.output_dir}")
        print(f"Supported languages: {', '.join(WikitextParser.SUPPORTED_LANGUAGES.keys())}")
        if max_pages:
            print(f"Limit: {max_pages:,} pages")
        print(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("="*70)
        print()

        last_report = datetime.now()

        try:
            # Use iterparse for memory efficiency
            context = ET.iterparse(self.xml_path, events=('end',))

            for event, elem in context:
                if elem.tag.endswith('page'):
                    self.stats['total_pages'] += 1

                    # Only process main namespace (0)
                    ns_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}ns')
                    if ns_elem is not None and ns_elem.text == '0':
                        self.stats['main_namespace'] += 1

                        # Extract title and text
                        title_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}title')
                        text_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}text')

                        if title_elem is not None and text_elem is not None and text_elem.text:
                            title = title_elem.text
                            wikitext = text_elem.text

                            # Process this entry for all supported languages
                            self.process_entry(title, wikitext)

                            # Check if we found any target language
                            if any(self.extract_language_section(wikitext, lang)
                                   for lang in WikitextParser.SUPPORTED_LANGUAGES.keys()):
                                self.stats['pages_with_target_languages'] += 1

                    # Progress reporting every 10 seconds
                    now = datetime.now()
                    if (now - last_report).seconds >= 10:
                        total_entries = sum(self.stats['entries_per_language'].values())
                        print(f"Progress: {self.stats['total_pages']:,} pages, "
                              f"{self.stats['main_namespace']:,} dictionary entries, "
                              f"{self.stats['pages_with_target_languages']:,} with target languages, "
                              f"{total_entries:,} entries generated")
                        last_report = now

                    # Clear element to free memory
                    elem.clear()

                    # Stop if max_pages reached
                    if max_pages and self.stats['total_pages'] >= max_pages:
                        print(f"\nReached max_pages limit: {max_pages:,}")
                        break

        except KeyboardInterrupt:
            print("\n\nProcessing interrupted by user.")
        except Exception as e:
            print(f"\n\nError during processing: {e}")
            import traceback
            traceback.print_exc()
        finally:
            # Close all file handles
            for f in self.language_files.values():
                f.close()

        self.print_summary()

    def print_summary(self) -> None:
        """Print processing summary."""
        print("\n" + "="*70)
        print("PROCESSING COMPLETE")
        print("="*70)
        print(f"Total pages processed: {self.stats['total_pages']:,}")
        print(f"Dictionary entries: {self.stats['main_namespace']:,}")
        print(f"Pages with target languages: {self.stats['pages_with_target_languages']:,}")
        print(f"Total entries generated: {sum(self.stats['entries_per_language'].values()):,}")

        print("\n" + "="*70)
        print("ENTRIES PER LANGUAGE")
        print("="*70)

        for lang in sorted(WikitextParser.SUPPORTED_LANGUAGES.keys()):
            count = self.stats['entries_per_language'][lang]
            errors = self.stats['errors_per_language'][lang]
            lang_code = WikitextParser.get_language_code(lang)

            if count > 0:
                error_pct = (errors / (count + errors) * 100) if (count + errors) > 0 else 0
                print(f"{lang_code:3} {lang:15} {count:,} entries, {errors:,} errors ({error_pct:.1f}%)")

        print(f"\nOutput files saved to: {self.output_dir}/")

        # Show file sizes
        print("\n" + "="*70)
        print("OUTPUT FILE SIZES")
        print("="*70)

        total_size = 0
        for lang in sorted(WikitextParser.SUPPORTED_LANGUAGES.keys()):
            lang_code = WikitextParser.get_language_code(lang)
            filepath = os.path.join(self.output_dir, f"{lang_code}.jsonl.gz")

            if os.path.exists(filepath):
                size = os.path.getsize(filepath)
                total_size += size
                size_mb = size / (1024 * 1024)
                print(f"{lang_code:3} {lang:15} {size_mb:,.1f} MB")

        print(f"\nTotal size: {total_size / (1024 * 1024):,.1f} MB")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description='Generate JSONL files for supported languages')
    parser.add_argument('--xml', type=str,
                       default='/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml',
                       help='Path to Wiktionary XML dump')
    parser.add_argument('--output', type=str,
                       default='/Users/andrii/Projects/vocabee/wiktionary_jsonl',
                       help='Output directory for JSONL files')
    parser.add_argument('--limit', type=int, default=None,
                       help='Limit number of pages to process (for testing)')

    args = parser.parse_args()

    generator = LanguageJsonlGenerator(args.xml, args.output)
    generator.process_dump(max_pages=args.limit)
