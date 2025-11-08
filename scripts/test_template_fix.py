#!/usr/bin/env python3
"""
Test script to verify the form-of template fix.
"""

import sys
sys.path.insert(0, '/Users/andrii/Projects/vocabee/scripts')

from wikitext_parser import WikitextParser

# Test case 1: récits (plural of récit)
test_wikitext = """==French==

===Noun===
{{head|fr|noun form|g=m}}

# {{plural of|fr|récit}}
"""

parser = WikitextParser("French", "récits", test_wikitext)
result = parser.parse()

print("=" * 70)
print("TEST: récits (plural of récit)")
print("=" * 70)

if result and len(result) > 0:
    entry = result[0]
    print(f"Word: {entry.get('word', entry.get('lemma'))}")
    print(f"Language: {entry.get('language')}")
    print(f"Is inflected form: {entry.get('is_inflected_form')}")

    if entry.get('is_inflected_form'):
        print(f"Inflected form of: {entry.get('inflected_form_of', {}).get('lemma')}")
    elif entry.get('definitions'):
        print(f"Definitions:")
        for defn in entry['definitions']:
            print(f"  {defn['definition_number']}. {defn['text']}")
    else:
        print("No definitions found")

    print("\nFull entry:")
    import json
    print(json.dumps(entry, indent=2, default=str))
else:
    print("ERROR: No results returned")

print("\n" + "=" * 70)

# Test case 2: Check if it properly extracts "récit" from the template
test_text = "# {{plural of|fr|récit}}"
parser2 = WikitextParser("French", "test", f"===Noun===\n{test_text}")
cleaned = parser2._clean_wikitext(test_text)
print(f"TEST: Template cleaning")
print(f"Input:  {test_text}")
print(f"Output: {cleaned}")
print(f"Expected: # plural of récit")
print(f"Success: {'✓' if 'récit' in cleaned else '✗'}")
print("=" * 70)
