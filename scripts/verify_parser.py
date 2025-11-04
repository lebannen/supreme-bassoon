#!/usr/bin/env python3
"""
Parser verification tool - helps validate parsing logic.

Features:
- Fetch raw XML for any word
- Show parsed JSON side-by-side
- Compare fields
- Find edge cases
"""

import sqlite3
import json
import sys
import os
from pathlib import Path

class ParserVerifier:
    def __init__(self, index_db_path, xml_path, parsed_json_dir=None):
        self.index_db_path = index_db_path
        self.xml_path = xml_path
        self.parsed_json_dir = parsed_json_dir

        if not os.path.exists(index_db_path):
            print(f"Error: Index not found: {index_db_path}")
            print("Run build_index.py first")
            sys.exit(1)

        self.conn = sqlite3.connect(index_db_path)
        self.cursor = self.conn.cursor()

    def get_raw_xml(self, title):
        """Get raw XML for a word."""
        self.cursor.execute('''
            SELECT page_id, namespace, byte_offset, byte_length
            FROM page_index
            WHERE title = ?
        ''', (title,))

        result = self.cursor.fetchone()
        if not result:
            return None

        page_id, namespace, byte_offset, byte_length = result

        with open(self.xml_path, 'rb') as f:
            f.seek(byte_offset)
            xml_content = f.read(byte_length)

        return xml_content.decode('utf-8', errors='ignore')

    def get_parsed_json(self, language, title):
        """Get parsed JSON for a word (if exists)."""
        if not self.parsed_json_dir:
            return None

        json_file = Path(self.parsed_json_dir) / f"{language}.jsonl"
        if not json_file.exists():
            return None

        # Search JSONL file for matching entry
        with open(json_file, 'r', encoding='utf-8') as f:
            for line in f:
                try:
                    entry = json.loads(line)
                    if entry.get('lemma') == title:
                        return entry
                except:
                    continue

        return None

    def verify_word(self, title, language=None):
        """
        Verify a word by showing raw XML and parsed JSON side-by-side.
        """
        print("="*80)
        print(f"VERIFICATION REPORT: {title}")
        print("="*80)

        # Get raw XML
        raw_xml = self.get_raw_xml(title)

        if not raw_xml:
            print(f"❌ Word not found in index: {title}")
            return False

        print("\n📄 RAW XML:")
        print("-"*80)
        # Show first 2000 characters
        print(raw_xml[:2000])
        if len(raw_xml) > 2000:
            print("\n... [truncated, full XML is", len(raw_xml), "characters]")

        # Try to get parsed JSON
        if language and self.parsed_json_dir:
            parsed = self.get_parsed_json(language, title)

            if parsed:
                print("\n" + "="*80)
                print("✅ PARSED JSON:")
                print("-"*80)
                print(json.dumps(parsed, indent=2, ensure_ascii=False))

                # Basic validation
                print("\n" + "="*80)
                print("VALIDATION CHECKS:")
                print("-"*80)

                checks = []
                checks.append(("Lemma matches", parsed.get('lemma') == title))
                checks.append(("Language set", bool(parsed.get('language'))))
                checks.append(("Has definitions", len(parsed.get('definitions', [])) > 0))
                checks.append(("Has part of speech", bool(parsed.get('part_of_speech'))))

                for check_name, passed in checks:
                    status = "✅" if passed else "❌"
                    print(f"  {status} {check_name}")

            else:
                print(f"\n⚠️  No parsed JSON found for {language}/{title}")
                print(f"   (Looked in: {self.parsed_json_dir}/{language}.jsonl)")

        print("\n" + "="*80)
        return True

    def random_sample(self, language, count=5):
        """
        Get random sample of entries for a language.
        """
        self.cursor.execute('''
            SELECT title
            FROM page_index
            WHERE namespace = '0'
            ORDER BY RANDOM()
            LIMIT ?
        ''', (count,))

        results = self.cursor.fetchall()

        print(f"Random sample of {count} entries:\n")
        for title, in results:
            # Try to determine if this word has the target language
            xml = self.get_raw_xml(title)
            if xml and f"=={language}==" in xml:
                print(f"  - {title}")

    def compare_fields(self, title, language, expected_fields):
        """
        Check if parsed JSON contains expected fields.
        """
        parsed = self.get_parsed_json(language, title)

        if not parsed:
            print(f"❌ No parsed data for {title}")
            return

        print(f"\nField comparison for: {title}")
        print("-"*80)

        for field, expected_value in expected_fields.items():
            actual_value = parsed.get(field)

            if actual_value == expected_value:
                print(f"  ✅ {field}: {actual_value}")
            else:
                print(f"  ❌ {field}:")
                print(f"     Expected: {expected_value}")
                print(f"     Actual:   {actual_value}")

    def interactive(self):
        """Interactive verification mode."""
        print("="*80)
        print("PARSER VERIFICATION TOOL - Interactive Mode")
        print("="*80)
        print("\nCommands:")
        print("  verify <word> [language]  - Verify a word")
        print("  sample <language> <count> - Get random samples")
        print("  raw <word>                - Show raw XML only")
        print("  quit                      - Exit")
        print("="*80)

        while True:
            try:
                cmd = input("\n> ").strip().split()

                if not cmd:
                    continue

                if cmd[0] == 'quit':
                    break

                elif cmd[0] == 'verify':
                    if len(cmd) < 2:
                        print("Usage: verify <word> [language]")
                        continue

                    word = cmd[1]
                    language = cmd[2] if len(cmd) > 2 else None
                    self.verify_word(word, language)

                elif cmd[0] == 'sample':
                    if len(cmd) < 3:
                        print("Usage: sample <language> <count>")
                        continue

                    language = cmd[1]
                    count = int(cmd[2])
                    self.random_sample(language, count)

                elif cmd[0] == 'raw':
                    if len(cmd) < 2:
                        print("Usage: raw <word>")
                        continue

                    word = cmd[1]
                    xml = self.get_raw_xml(word)
                    if xml:
                        print("\n" + "="*80)
                        print(f"RAW XML: {word}")
                        print("="*80)
                        print(xml)
                    else:
                        print(f"Word not found: {word}")

                else:
                    print(f"Unknown command: {cmd[0]}")

            except KeyboardInterrupt:
                print("\nExiting...")
                break
            except Exception as e:
                print(f"Error: {e}")

    def close(self):
        self.conn.close()


def main():
    index_db_path = "/Users/andrii/Projects/vocabee/wiktionary_index.db"
    xml_path = "/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml"
    parsed_json_dir = "/Users/andrii/Projects/vocabee/parsed_data"  # Optional

    verifier = ParserVerifier(index_db_path, xml_path, parsed_json_dir)

    if len(sys.argv) == 1 or sys.argv[1] == '--interactive':
        verifier.interactive()

    elif sys.argv[1] == 'verify':
        if len(sys.argv) < 3:
            print("Usage: python verify_parser.py verify <word> [language]")
            sys.exit(1)

        word = sys.argv[2]
        language = sys.argv[3] if len(sys.argv) > 3 else None
        verifier.verify_word(word, language)

    elif sys.argv[1] == 'raw':
        if len(sys.argv) < 3:
            print("Usage: python verify_parser.py raw <word>")
            sys.exit(1)

        word = sys.argv[2]
        xml = verifier.get_raw_xml(word)
        if xml:
            print(xml)
        else:
            print(f"Word not found: {word}")
            sys.exit(1)

    else:
        print("Usage:")
        print("  python verify_parser.py --interactive")
        print("  python verify_parser.py verify <word> [language]")
        print("  python verify_parser.py raw <word>")
        sys.exit(1)

    verifier.close()


if __name__ == "__main__":
    main()
