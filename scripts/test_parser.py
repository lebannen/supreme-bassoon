#!/usr/bin/env python3
"""
Test the wikitext parser on real Wiktionary entries.

Fetches entries using the index and parses them.
"""

import sys
import re
import json
import xml.etree.ElementTree as ET
from pathlib import Path

# Add scripts directory to path
sys.path.insert(0, str(Path(__file__).parent))

from query_entry import WiktionaryQuery
from wikitext_parser import parse_entry


def extract_language_section(wikitext: str, language: str) -> str:
    """
    Extract a specific language section from the full wikitext.

    Args:
        wikitext: Full wikitext content
        language: Language name to extract

    Returns:
        Wikitext for that language section only
    """
    # Pattern to match language sections
    pattern = rf'^==\s*{re.escape(language)}\s*==\s*$(.*?)(?=^==\s*[^=]|\Z)'
    match = re.search(pattern, wikitext, re.MULTILINE | re.DOTALL)

    if match:
        return match.group(1).strip()

    return None


def parse_xml_to_wikitext(xml_content: str) -> str:
    """
    Extract wikitext from XML page.
    """
    # Parse XML
    try:
        # Wrap in root element if needed
        if not xml_content.strip().startswith('<?xml'):
            xml_content = '<?xml version="1.0"?><root>' + xml_content + '</root>'

        root = ET.fromstring(xml_content)

        # Find text element (handles different namespace scenarios)
        text_elem = root.find('.//text')
        if text_elem is None:
            # Try with namespace
            text_elem = root.find('.//{*}text')

        if text_elem is not None and text_elem.text:
            return text_elem.text

    except ET.ParseError as e:
        print(f"XML Parse Error: {e}")

    return None


def test_word(word: str, language: str = "Spanish"):
    """
    Test parsing a specific word.
    """
    print(f"Testing parser on: {word} ({language})")
    print("="*70)

    # Initialize query tool
    index_db_path = "/Users/andrii/Projects/vocabee/wiktionary_index.db"
    xml_path = "/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml"

    query = WiktionaryQuery(index_db_path, xml_path)

    # Fetch entry
    entry = query.get_entry(word)

    if not entry:
        print(f"❌ Word not found: {word}")
        return

    print(f"✅ Found entry: {entry['title']} (Page ID: {entry['page_id']})")
    print()

    # Extract wikitext from XML
    wikitext = parse_xml_to_wikitext(entry['xml'])

    if not wikitext:
        print("❌ Could not extract wikitext from XML")
        return

    # Extract language section
    lang_section = extract_language_section(wikitext, language)

    if not lang_section:
        print(f"❌ No {language} section found")
        print(f"\nAvailable languages:")
        # Show available languages
        langs = re.findall(r'^==([^=\n]+)==', wikitext, re.MULTILINE)
        for lang in langs:
            print(f"  - {lang.strip()}")
        return

    print(f"✅ Extracted {language} section ({len(lang_section)} characters)")
    print()

    # Show section preview
    print("Section preview (first 500 chars):")
    print("-"*70)
    print(lang_section[:500])
    print("...[truncated]" if len(lang_section) > 500 else "")
    print()

    # Parse the section
    print("="*70)
    print("PARSED OUTPUT:")
    print("="*70)

    try:
        parsed = parse_entry(language, word, lang_section)
        print(json.dumps(parsed, indent=2, ensure_ascii=False))

        # Validation summary
        print()
        print("="*70)
        print("VALIDATION:")
        print("="*70)

        checks = [
            ("Part of speech", parsed.get('part_of_speech') is not None),
            ("Definitions", len(parsed.get('definitions', [])) > 0),
            ("Etymology", parsed.get('etymology') is not None),
            ("Pronunciations", len(parsed.get('pronunciations', [])) > 0),
        ]

        for check_name, passed in checks:
            status = "✅" if passed else "❌"
            print(f"  {status} {check_name}")

    except Exception as e:
        print(f"❌ Parser error: {e}")
        import traceback
        traceback.print_exc()

    query.close()


def interactive_test():
    """
    Interactive testing mode.
    """
    print("="*70)
    print("WIKITEXT PARSER - Interactive Test")
    print("="*70)
    print()
    print("Commands:")
    print("  test <word> [language]  - Test parser on a word")
    print("  quit                    - Exit")
    print("="*70)

    while True:
        try:
            cmd = input("\n> ").strip().split()

            if not cmd:
                continue

            if cmd[0] == 'quit':
                break

            if cmd[0] == 'test':
                if len(cmd) < 2:
                    print("Usage: test <word> [language]")
                    continue

                word = cmd[1]
                language = cmd[2] if len(cmd) > 2 else "Spanish"

                print()
                test_word(word, language)

            else:
                print(f"Unknown command: {cmd[0]}")

        except KeyboardInterrupt:
            print("\nExiting...")
            break
        except Exception as e:
            print(f"Error: {e}")


def main():
    if len(sys.argv) == 1:
        # Interactive mode
        interactive_test()
    elif len(sys.argv) >= 2:
        # Direct test
        word = sys.argv[1]
        language = sys.argv[2] if len(sys.argv) > 2 else "Spanish"
        test_word(word, language)
    else:
        print("Usage:")
        print("  python test_parser.py                    # Interactive mode")
        print("  python test_parser.py <word> [language]  # Direct test")


if __name__ == "__main__":
    main()
