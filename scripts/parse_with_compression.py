#!/usr/bin/env python3
"""
Example: Parse Wiktionary entries and save as compressed JSONL.

Shows how to write compressed output during parsing.
"""

import json
import gzip
from pathlib import Path
from query_entry import WiktionaryQuery
from test_parser import extract_language_section, parse_xml_to_wikitext
from wikitext_parser import parse_entry


def parse_language_to_jsonl_gz(
    output_file: str,
    language: str,
    index_db: str,
    xml_file: str,
    limit: int = None
):
    """
    Parse all entries for a language and save as compressed JSONL.

    Args:
        output_file: Path to output .jsonl.gz file
        language: Language to extract (e.g., "Spanish")
        index_db: Path to index database
        xml_file: Path to Wiktionary XML file
        limit: Optional limit on number of entries to process
    """
    query = WiktionaryQuery(index_db, xml_file)

    # Open gzip file for writing
    with gzip.open(output_file, 'wt', encoding='utf-8', compresslevel=9) as f:
        count = 0

        # In practice, you'd query the index for specific language entries
        # For demo, just showing the pattern
        for title in ['correr', 'hablar', 'casa', 'perro']:  # Example words
            if limit and count >= limit:
                break

            entry = query.get_entry(title)
            if not entry:
                continue

            wikitext = parse_xml_to_wikitext(entry['xml'])
            lang_section = extract_language_section(wikitext, language)

            if not lang_section:
                continue

            # Parse the entry
            parsed = parse_entry(language, title, lang_section)

            # Write as single line JSON
            f.write(json.dumps(parsed, ensure_ascii=False) + '\n')
            count += 1

            if count % 1000 == 0:
                print(f'Processed {count} entries...')

    query.close()
    print(f'✅ Saved {count} entries to {output_file}')


def read_jsonl_gz(file_path: str, limit: int = 5):
    """
    Read and display entries from compressed JSONL file.
    """
    print(f'\nReading from {file_path}:')
    print('='*70)

    with gzip.open(file_path, 'rt', encoding='utf-8') as f:
        for i, line in enumerate(f):
            if i >= limit:
                break

            entry = json.loads(line)
            lemma = entry.get('lemma') or entry.get('word')
            is_inflected = entry.get('is_inflected_form', False)
            entry_type = 'inflected' if is_inflected else 'lemma'

            print(f'{i+1}. {lemma} ({entry_type})')


if __name__ == '__main__':
    # Example usage
    index_db = '/Users/andrii/Projects/vocabee/wiktionary_index.db'
    xml_file = '/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml'

    output = 'spanish_sample.jsonl.gz'

    print('Parsing Spanish entries with compression...')
    parse_language_to_jsonl_gz(
        output_file=output,
        language='Spanish',
        index_db=index_db,
        xml_file=xml_file,
        limit=10  # Just a few for demo
    )

    # Read it back
    read_jsonl_gz(output)

    # Show file size
    import os
    size = os.path.getsize(output)
    print(f'\nFile size: {size:,} bytes ({size/1024:.2f} KB)')
