#!/usr/bin/env python3
"""
Test the parser fixes for avoir (multiple POS) and fait (inflected form)
"""

import subprocess
import json
from wikitext_parser import parse_entry

def extract_language_section(wikitext, language):
    """Extract a specific language section from wikitext."""
    pattern = f"=={language}=="
    if pattern not in wikitext:
        return None

    start = wikitext.find(pattern)
    # Find next language section (==SomeLanguage==)
    next_section = wikitext.find("\n==", start + len(pattern))

    if next_section == -1:
        return wikitext[start:]
    else:
        return wikitext[start:next_section]

def test_avoir():
    """Test avoir - should create 2 entries (noun + verb)"""
    print("=" * 70)
    print("TEST 1: avoir (multiple POS sections)")
    print("=" * 70)

    # Get XML from query tool
    result = subprocess.run(
        ['python3', 'query_entry.py', 'avoir'],
        capture_output=True,
        text=True,
        cwd='/Users/andrii/Projects/vocabee/scripts'
    )

    if result.returncode != 0:
        print("❌ Failed to fetch avoir")
        return

    # Extract wikitext from XML
    xml_content = result.stdout
    text_start = xml_content.find('<text bytes=')
    if text_start == -1:
        print("❌ Could not find wikitext in XML")
        return

    text_start = xml_content.find('>', text_start) + 1
    text_end = xml_content.find('</text>', text_start)
    wikitext = xml_content[text_start:text_end]

    # Extract French section
    french_section = extract_language_section(wikitext, "French")

    if not french_section:
        print("❌ Could not find French section")
        return

    # Parse
    try:
        entries = parse_entry("French", "avoir", french_section)
        print(f"✅ Parsed {len(entries)} entries")

        for i, entry in enumerate(entries):
            print(f"\nEntry {i+1}:")
            print(f"  POS: {entry.get('part_of_speech')}")
            print(f"  Definitions: {len(entry.get('definitions', []))}")
            if entry.get('definitions'):
                first_def = entry['definitions'][0]['text'][:60]
                print(f"  First def: {first_def}...")

        # Check results
        if len(entries) == 2:
            poss = [e.get('part_of_speech') for e in entries]
            if 'noun' in poss and 'verb' in poss:
                print("\n✅ SUCCESS: Found both noun and verb entries!")
            else:
                print(f"\n⚠️  WARNING: Expected noun+verb, got: {poss}")
        else:
            print(f"\n⚠️  WARNING: Expected 2 entries, got {len(entries)}")

    except Exception as e:
        print(f"❌ Parse error: {e}")
        import traceback
        traceback.print_exc()

def test_fait():
    """Test fait - should be inflected form with inflected_form_of structure"""
    print("\n" + "=" * 70)
    print("TEST 2: fait (inflected form linking)")
    print("=" * 70)

    # Get XML from query tool
    result = subprocess.run(
        ['python3', 'query_entry.py', 'fait'],
        capture_output=True,
        text=True,
        cwd='/Users/andrii/Projects/vocabee/scripts'
    )

    if result.returncode != 0:
        print("❌ Failed to fetch fait")
        return

    # Extract wikitext from XML
    xml_content = result.stdout
    text_start = xml_content.find('<text bytes=')
    if text_start == -1:
        print("❌ Could not find wikitext in XML")
        return

    text_start = xml_content.find('>', text_start) + 1
    text_end = xml_content.find('</text>', text_start)
    wikitext = xml_content[text_start:text_end]

    # Extract French section
    french_section = extract_language_section(wikitext, "French")

    if not french_section:
        print("❌ Could not find French section")
        return

    # Parse
    try:
        entries = parse_entry("French", "fait", french_section)
        print(f"✅ Parsed {len(entries)} entries")

        for i, entry in enumerate(entries):
            print(f"\nEntry {i+1}:")
            print(f"  Word: {entry.get('word', entry.get('lemma'))}")
            print(f"  Is inflected: {entry.get('is_inflected_form')}")
            print(f"  POS: {entry.get('part_of_speech')}")

            if entry.get('is_inflected_form'):
                inf_of = entry.get('inflected_form_of')
                if inf_of:
                    print(f"  Inflected form of:")
                    print(f"    lemma: {inf_of.get('lemma')}")
                    print(f"    person: {inf_of.get('person')}")
                    print(f"    number: {inf_of.get('number')}")
                    print(f"    tense: {inf_of.get('tense')}")
                else:
                    print("  ⚠️  WARNING: No inflected_form_of field!")

        # Check results
        if entries and entries[0].get('is_inflected_form'):
            inf_of = entries[0].get('inflected_form_of', {})
            if inf_of and inf_of.get('lemma'):
                print(f"\n✅ SUCCESS: Inflected form structure correct!")
                print(f"   Links to lemma: {inf_of.get('lemma')}")
            else:
                print("\n⚠️  WARNING: inflected_form_of field missing or empty")
        else:
            print("\n⚠️  WARNING: Expected inflected form entry")

    except Exception as e:
        print(f"❌ Parse error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_avoir()
    test_fait()
