#!/usr/bin/env python3
"""
Extract sample entries from Wiktionary dump for specific languages.
This helps understand the data structure and available information.
"""

import xml.etree.ElementTree as ET
import re
import sys

def parse_language_sections(wikitext, target_languages):
    """
    Extract specific language sections from wikitext.
    Returns dict of {language: section_text}
    """
    if not wikitext:
        return {}

    results = {}

    # Split by level-2 headings (==LanguageName==)
    # This pattern captures the language name and everything until the next level-2 heading
    pattern = r'^==([^=\n]+)==\s*$(.+?)(?=^==([^=\n]+)==|\Z)'
    matches = re.finditer(pattern, wikitext, re.MULTILINE | re.DOTALL)

    for match in matches:
        lang = match.group(1).strip()
        content = match.group(2).strip()

        if lang in target_languages:
            results[lang] = content

    return results

def extract_samples(filepath, target_languages, samples_per_language=5):
    """
    Extract sample entries for each target language.
    """
    print(f"Extracting samples for languages: {', '.join(target_languages)}\n")
    print(f"Target: {samples_per_language} samples per language\n")

    # Track samples found
    samples = {lang: [] for lang in target_languages}

    try:
        context = ET.iterparse(filepath, events=('end',))

        for event, elem in context:
            if elem.tag.endswith('page'):
                # Find namespace
                ns_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}ns')

                if ns_elem is not None and ns_elem.text == '0':
                    title_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}title')
                    text_elem = elem.find('.//{http://www.mediawiki.org/xml/export-0.11/}text')

                    if title_elem is not None and text_elem is not None and text_elem.text:
                        title = title_elem.text

                        # Extract language sections
                        lang_sections = parse_language_sections(text_elem.text, target_languages)

                        for lang, content in lang_sections.items():
                            if len(samples[lang]) < samples_per_language:
                                samples[lang].append({
                                    'title': title,
                                    'content': content
                                })

                # Check if we have enough samples
                if all(len(samples[lang]) >= samples_per_language for lang in target_languages):
                    print("Collected enough samples for all languages!")
                    break

                elem.clear()

    except KeyboardInterrupt:
        print("\n\nExtraction interrupted by user.")
    except Exception as e:
        print(f"\n\nError during extraction: {e}")

    # Print samples
    for lang in target_languages:
        print("\n" + "="*70)
        print(f"SAMPLES FOR: {lang}")
        print("="*70)

        if not samples[lang]:
            print("No samples found")
            continue

        for i, sample in enumerate(samples[lang], 1):
            print(f"\n--- Sample {i}: {sample['title']} ---")
            # Print first 1500 chars of content
            content = sample['content']
            if len(content) > 1500:
                content = content[:1500] + "\n...[truncated]"
            print(content)

    # Save to file
    output_file = "wiktionary_samples.txt"
    with open(output_file, 'w', encoding='utf-8') as f:
        for lang in target_languages:
            f.write("="*70 + "\n")
            f.write(f"SAMPLES FOR: {lang}\n")
            f.write("="*70 + "\n\n")

            for i, sample in enumerate(samples[lang], 1):
                f.write(f"\n{'='*70}\n")
                f.write(f"Sample {i}: {sample['title']}\n")
                f.write(f"{'='*70}\n\n")
                f.write(sample['content'])
                f.write("\n\n")

    print(f"\n\nFull samples saved to: {output_file}")

if __name__ == "__main__":
    dump_path = "/Users/andrii/Downloads/enwiktionary-latest-pages-articles.xml"

    # Focus on top practical languages for language learning
    target_languages = [
        "Spanish",
        "French",
        "German",
        "Italian",
        "Russian",
        "Japanese",
        "Portuguese",
        "Chinese"
    ]

    extract_samples(dump_path, target_languages, samples_per_language=3)
