#!/usr/bin/env python3
"""
Wiktionary XML dump parser to extract language statistics.
This script parses the entire dump and counts entries per language.
"""

import xml.etree.ElementTree as ET
import re
import sys
from collections import defaultdict
from datetime import datetime

def parse_language_sections(wikitext):
    """
    Extract language names from wikitext.
    Language sections are marked with ==LanguageName==
    """
    if not wikitext:
        return []

    # Match ==LanguageName== at the start of a line
    # This regex captures level-2 headings which indicate language sections
    pattern = r'^==([^=\n]+)==\s*$'
    matches = re.findall(pattern, wikitext, re.MULTILINE)

    # Clean up language names (remove extra whitespace)
    languages = [lang.strip() for lang in matches]
    return languages

def parse_dump(filepath):
    """
    Parse the entire Wiktionary dump and collect statistics.
    Uses iterative parsing to handle large files efficiently.
    """
    print(f"Starting parse of: {filepath}")
    print(f"Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    # Statistics
    language_counts = defaultdict(int)
    total_pages = 0
    main_namespace_pages = 0
    pages_with_languages = 0

    # Progress tracking
    last_report = datetime.now()
    report_interval = 10  # seconds

    try:
        # Use iterparse for memory-efficient streaming
        context = ET.iterparse(filepath, events=('end',))

        for event, elem in context:
            if elem.tag.endswith('page'):
                total_pages += 1

                # Find namespace
                ns_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}ns')
                if ns_elem is not None and ns_elem.text == '0':
                    main_namespace_pages += 1

                    # Find title and text
                    title_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}title')
                    text_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}text')

                    if text_elem is not None and text_elem.text:
                        languages = parse_language_sections(text_elem.text)

                        if languages:
                            pages_with_languages += 1
                            for lang in languages:
                                language_counts[lang] += 1

                # Progress reporting
                now = datetime.now()
                if (now - last_report).seconds >= report_interval:
                    print(f"Progress: {total_pages:,} pages processed, "
                          f"{main_namespace_pages:,} dictionary entries, "
                          f"{len(language_counts)} languages found")
                    last_report = now

                # Clear element to free memory
                elem.clear()

    except KeyboardInterrupt:
        print("\n\nParsing interrupted by user.")
        print("Showing statistics for data processed so far...\n")
    except Exception as e:
        print(f"\n\nError during parsing: {e}")
        print("Showing statistics for data processed so far...\n")

    # Print final statistics
    print("\n" + "="*70)
    print("PARSING COMPLETE")
    print("="*70)
    print(f"Total pages in dump: {total_pages:,}")
    print(f"Dictionary entries (namespace 0): {main_namespace_pages:,}")
    print(f"Entries with language sections: {pages_with_languages:,}")
    print(f"Unique languages found: {len(language_counts)}")
    print("\n" + "="*70)
    print("TOP 50 LANGUAGES BY ENTRY COUNT")
    print("="*70)

    # Sort languages by count
    sorted_languages = sorted(language_counts.items(), key=lambda x: x[1], reverse=True)

    for i, (lang, count) in enumerate(sorted_languages[:50], 1):
        print(f"{i:2}. {lang:30} {count:,} entries")

    # Save full results to file
    output_file = "wiktionary_language_stats.txt"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("Wiktionary Language Statistics\n")
        f.write("="*70 + "\n\n")
        f.write(f"Total pages: {total_pages:,}\n")
        f.write(f"Dictionary entries: {main_namespace_pages:,}\n")
        f.write(f"Entries with languages: {pages_with_languages:,}\n")
        f.write(f"Unique languages: {len(language_counts)}\n\n")
        f.write("="*70 + "\n")
        f.write("ALL LANGUAGES (sorted by entry count)\n")
        f.write("="*70 + "\n\n")

        for i, (lang, count) in enumerate(sorted_languages, 1):
            f.write(f"{i:3}. {lang:40} {count:,}\n")

    print(f"\nFull results saved to: {output_file}")

    return language_counts

if __name__ == "__main__":
    dump_path = "/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml"
    parse_dump(dump_path)