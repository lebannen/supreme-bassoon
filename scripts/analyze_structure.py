#!/usr/bin/env python3
"""
Analyze and visualize Wiktionary XML structure.
"""

import xml.etree.ElementTree as ET
import re

def analyze_entry_structure(filepath, word_to_find="gratis"):
    """
    Find and display the complete structure of a specific word entry.
    """
    print(f"Searching for word: {word_to_find}\n")

    context = ET.iterparse(filepath, events=('end',))

    for event, elem in context:
        if elem.tag.endswith('page'):
            title_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}title')

            if title_elem is not None and title_elem.text == word_to_find:
                # Found it!
                ns_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}ns')
                text_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}text')
                id_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}id')

                print("="*70)
                print(f"FOUND: {title_elem.text}")
                print("="*70)
                print(f"Page ID: {id_elem.text if id_elem is not None else 'N/A'}")
                print(f"Namespace: {ns_elem.text if ns_elem is not None else 'N/A'}")
                print()

                if text_elem is not None and text_elem.text:
                    wikitext = text_elem.text

                    # Analyze language sections
                    print("LANGUAGE SECTIONS FOUND:")
                    print("-"*70)

                    # Find all level-2 headings (languages)
                    pattern = r'^==([^=\n]+)==\s*$'
                    languages = re.findall(pattern, wikitext, re.MULTILINE)

                    for i, lang in enumerate(languages, 1):
                        print(f"{i}. {lang.strip()}")

                    print()
                    print("="*70)
                    print("COMPLETE WIKITEXT CONTENT:")
                    print("="*70)
                    print(wikitext)
                    print("\n" + "="*70)

                    # Now let's analyze the structure by splitting into sections
                    print("\nSTRUCTURE ANALYSIS:")
                    print("="*70)

                    # Split by language sections
                    sections = re.split(r'^(==.+?==)\s*$', wikitext, flags=re.MULTILINE)

                    current_lang = None
                    for section in sections:
                        # Check if this is a language header
                        lang_match = re.match(r'^==([^=]+)==\s*$', section.strip())
                        if lang_match:
                            current_lang = lang_match.group(1).strip()
                            print(f"\n{'='*70}")
                            print(f"LANGUAGE: {current_lang}")
                            print(f"{'='*70}")
                        elif current_lang and section.strip():
                            # This is content for the current language
                            print(f"\nContent length: {len(section)} characters")
                            print(f"First 500 chars:\n{section[:500]}")
                            if len(section) > 500:
                                print("... [truncated]")

                return True

            elem.clear()

    print(f"Word '{word_to_find}' not found in dump")
    return False

if __name__ == "__main__":
    dump_path = "/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml"
    analyze_entry_structure(dump_path, "gratis")
