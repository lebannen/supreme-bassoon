#!/usr/bin/env python3
"""
Wikitext parser for Wiktionary entries.

Converts MediaWiki wikitext to structured JSON format.
Handles templates, definitions, pronunciations, word forms, etc.
"""

import re
import json
from typing import Dict, List, Optional, Any
from datetime import datetime


class WikitextParser:
    """
    Parser for Wiktionary wikitext format.

    Extracts structured data from a language section of a Wiktionary entry.
    """

    def __init__(self, language: str, lemma: str, wikitext: str):
        self.language = language
        self.lemma = lemma
        self.wikitext = wikitext

    def parse(self) -> List[Dict[str, Any]]:
        """
        Parse the wikitext and return structured data.

        Detects if this is an inflected form entry or a lemma entry
        and parses accordingly. For lemma entries with multiple POS sections,
        returns one entry per POS.

        Returns:
            List of entries (one per POS section for lemmas, or one for inflected forms)
        """
        # Check if this is an inflected form
        inflected_info = self._detect_inflected_form()

        if inflected_info:
            # Parse as inflected form - return single entry
            result = {
                "language": self.language,
                "word": self.lemma,
                "is_inflected_form": True,
                "inflected_form_of": {
                    "lemma": inflected_info["lemma"],
                    "part_of_speech": inflected_info.get("part_of_speech"),
                    "person": inflected_info["features"].get("person"),
                    "number": inflected_info["features"].get("number"),
                    "tense": inflected_info["features"].get("tense"),
                    "mood": inflected_info["features"].get("mood"),
                    "gender": inflected_info["features"].get("gender"),
                    "case": inflected_info["features"].get("case")
                },
                "part_of_speech": inflected_info.get("part_of_speech"),
                "grammatical_features": inflected_info["features"],
                "pronunciations": self._extract_pronunciations(),
                "metadata": {
                    "parsed_at": datetime.now().isoformat(),
                    "parser_version": "0.2.0"
                }
            }
            return [result]
        else:
            # Parse as lemma - split by POS sections
            return self._parse_pos_sections()

    def _parse_pos_sections(self) -> List[Dict[str, Any]]:
        """
        Parse multiple POS sections and return one entry per POS.

        For entries like "avoir" which has both ===Noun=== and ===Verb===,
        this creates separate JSON entries for each.
        """
        entries = []

        # Find all POS section headers
        pos_pattern = r'^===\s*(Noun|Verb|Adjective|Adverb|Pronoun|Preposition|Conjunction|Interjection|Particle|Determiner|Article|Numeral|Proper noun|Phrase)\s*===\s*$'

        # Split wikitext by POS headers
        lines = self.wikitext.split('\n')
        current_pos = None
        current_section_lines = []

        # Extract etymology and pronunciation once (shared across POS)
        etymology = self._extract_etymology()
        pronunciations = self._extract_pronunciations()

        for i, line in enumerate(lines):
            pos_match = re.match(pos_pattern, line, re.IGNORECASE)

            if pos_match:
                # Save previous section if any
                if current_pos and current_section_lines:
                    section_text = '\n'.join(current_section_lines)
                    entry = self._parse_single_pos_section(
                        current_pos, section_text, etymology, pronunciations
                    )
                    if entry:
                        entries.append(entry)

                # Start new section
                current_pos = pos_match.group(1).strip()
                current_section_lines = [line]
            elif current_pos:
                # Stop at next level-3 header that's not a POS
                if re.match(r'^===\s*[^=]', line) and not pos_match:
                    # Save current section and stop
                    if current_section_lines:
                        section_text = '\n'.join(current_section_lines)
                        entry = self._parse_single_pos_section(
                            current_pos, section_text, etymology, pronunciations
                        )
                        if entry:
                            entries.append(entry)
                    current_pos = None
                    current_section_lines = []
                else:
                    current_section_lines.append(line)

        # Handle last section
        if current_pos and current_section_lines:
            section_text = '\n'.join(current_section_lines)
            entry = self._parse_single_pos_section(
                current_pos, section_text, etymology, pronunciations
            )
            if entry:
                entries.append(entry)

        # If no POS sections found, parse as single entry (legacy behavior)
        if not entries:
            entry = {
                "language": self.language,
                "lemma": self.lemma,
                "is_inflected_form": False,
                "part_of_speech": self._extract_pos(),
                "pronunciations": pronunciations,
                "etymology": etymology,
                "definitions": self._extract_definitions(),
                "word_forms": self._extract_word_forms(),
                "related_words": [],
                "translations": {},
                "metadata": {
                    "parsed_at": datetime.now().isoformat(),
                    "parser_version": "0.2.0"
                }
            }
            entries.append(entry)

        return entries

    def _parse_single_pos_section(
        self,
        pos: str,
        section_text: str,
        etymology: Optional[str],
        pronunciations: List[Dict[str, Any]]
    ) -> Optional[Dict[str, Any]]:
        """
        Parse a single POS section.
        """
        # Normalize POS
        pos_map = {
            'Noun': 'noun',
            'Verb': 'verb',
            'Adjective': 'adjective',
            'Adverb': 'adverb',
            'Pronoun': 'pronoun',
            'Preposition': 'preposition',
            'Conjunction': 'conjunction',
            'Interjection': 'interjection',
            'Particle': 'particle',
            'Determiner': 'determiner',
            'Article': 'article',
            'Numeral': 'numeral',
            'Proper noun': 'proper_noun',
            'Phrase': 'phrase',
        }

        normalized_pos = pos_map.get(pos, pos.lower())

        # Create a temporary parser with just this section
        temp_parser = WikitextParser(self.language, self.lemma, section_text)
        definitions = temp_parser._extract_definitions()
        word_forms = temp_parser._extract_word_forms()

        # Skip empty sections (no definitions)
        if not definitions:
            return None

        return {
            "language": self.language,
            "lemma": self.lemma,
            "is_inflected_form": False,
            "part_of_speech": normalized_pos,
            "pronunciations": pronunciations,
            "etymology": etymology,
            "definitions": definitions,
            "word_forms": word_forms,
            "related_words": [],
            "translations": {},
            "metadata": {
                "parsed_at": datetime.now().isoformat(),
                "parser_version": "0.2.0"
            }
        }

    def _detect_inflected_form(self) -> Optional[Dict[str, Any]]:
        """
        Detect if this entry is an inflected form and extract information.

        Returns None if this is a lemma (main entry).
        Returns dict with lemma, form_type, and features if inflected form.
        """
        # First check for {{head|lang|verb form}}, {{head|lang|noun form}}, etc. to extract POS
        pos_from_head = None
        head_match = re.search(r'\{\{head\|[^|]+\|(verb|noun|adj|adv|participle|gerund) form\}\}', self.wikitext)
        if head_match:
            form_type = head_match.group(1)
            # Map form type to part of speech
            if form_type in ('verb', 'participle', 'gerund'):
                pos_from_head = 'verb'
            elif form_type == 'noun':
                pos_from_head = 'noun'
            elif form_type == 'adj':
                pos_from_head = 'adjective'
            elif form_type == 'adv':
                pos_from_head = 'adverb'

        # Look for form-of templates:
        # {{verb form of|de|laufen||1|s|pres}}
        # {{noun form of|...}}
        # {{inflection of|...}}
        # {{es-verb form of|...}}

        form_of_patterns = [
            # Standard form-of templates
            r'\{\{(verb|noun|adj|adv) form of\|([^|]+)\|([^}]+)\}\}',
            # Language-specific verb form templates
            r'\{\{([a-z]{2,3})-verb form of\|([^}]+)\}\}',
            # Generic inflection of
            r'\{\{inflection of\|([^|]+)\|([^|]+)\|([^}]+)\}\}',
            # Past participle of
            r'\{\{past participle of\|([^|]+)\|([^}]+)\}\}',
            # Gerund of, present participle of
            r'\{\{(gerund|present participle) of\|([^|]+)\|([^}]+)\}\}',
        ]

        for pattern in form_of_patterns:
            match = re.search(pattern, self.wikitext)
            if match:
                result = self._parse_form_of_template(match.group(0))
                # If form-of template didn't provide POS, use the one from head template
                if result and not result.get("part_of_speech") and pos_from_head:
                    result["part_of_speech"] = pos_from_head
                return result

        # If we have head template indicating form but no form-of template, try broader search
        if head_match:
            form_of_match = re.search(r'\{\{[^}]*form of[^}]+\}\}', self.wikitext, re.IGNORECASE)
            if form_of_match:
                result = self._parse_form_of_template(form_of_match.group(0))
                if result and not result.get("part_of_speech") and pos_from_head:
                    result["part_of_speech"] = pos_from_head
                return result

        return None

    def _parse_form_of_template(self, template: str) -> Dict[str, Any]:
        """
        Parse a form-of template to extract lemma and grammatical features.

        Examples:
        - {{verb form of|de|laufen||1|s|pres}} -> laufen, person=1, number=s, tense=pres
        - {{es-verb form of|correr}} -> correr
        - {{past participle of|de|laufen}} -> laufen, participle=past
        """
        result = {
            "lemma": None,
            "form_type": None,
            "features": {}
        }

        # Extract the lemma (usually 2nd or 3rd parameter)
        # Format: {{template|lang|lemma|...}} or {{template|lemma|...}}

        # Remove {{ and }}
        template = template.strip('{}')
        parts = [p.strip() for p in template.split('|')]

        if not parts:
            return result

        template_name = parts[0]

        # Determine form type and part of speech from template name
        if 'verb form' in template_name:
            result["form_type"] = "verb_form"
            result["part_of_speech"] = "verb"
        elif 'noun form' in template_name:
            result["form_type"] = "noun_form"
            result["part_of_speech"] = "noun"
        elif 'adj form' in template_name or 'adjective form' in template_name:
            result["form_type"] = "adjective_form"
            result["part_of_speech"] = "adjective"
        elif 'past participle' in template_name:
            result["form_type"] = "participle"
            result["part_of_speech"] = "verb"
            result["features"]["participle"] = "past"
        elif 'present participle' in template_name or 'gerund' in template_name:
            result["form_type"] = "participle"
            result["part_of_speech"] = "verb"
            result["features"]["participle"] = "present"
        else:
            result["form_type"] = "inflected_form"

        # Extract lemma - skip language code if present
        lemma_index = 1
        if len(parts) > 2 and len(parts[1]) <= 3:  # Likely a language code
            lemma_index = 2

        if len(parts) > lemma_index:
            result["lemma"] = parts[lemma_index]

        # Parse grammatical features from remaining parameters
        # Common abbreviations:
        # 1/2/3 = person, s/p = singular/plural, pres/past/fut = tense
        # ind/sub/imp = indicative/subjunctive/imperative

        feature_map = {
            '1': ('person', '1'),
            '2': ('person', '2'),
            '3': ('person', '3'),
            's': ('number', 'singular'),
            'p': ('number', 'plural'),
            'sg': ('number', 'singular'),
            'pl': ('number', 'plural'),
            'pres': ('tense', 'present'),
            'past': ('tense', 'past'),
            'pret': ('tense', 'preterite'),
            'fut': ('tense', 'future'),
            'impf': ('tense', 'imperfect'),
            'perf': ('aspect', 'perfect'),
            'ind': ('mood', 'indicative'),
            'sub': ('mood', 'subjunctive'),
            'subj': ('mood', 'subjunctive'),
            'imp': ('mood', 'imperative'),
            'cond': ('mood', 'conditional'),
            'inf': ('form', 'infinitive'),
            'm': ('gender', 'masculine'),
            'f': ('gender', 'feminine'),
            'n': ('gender', 'neuter'),
        }

        # Process remaining parameters (skip empty ones)
        for i in range(lemma_index + 1, len(parts)):
            param = parts[i].strip()
            if not param or '=' in param:  # Skip named parameters for now
                continue

            # Handle combined forms like "1//3" (1st or 3rd person)
            if '//' in param:
                param = param.split('//')[0]  # Take first option

            if param in feature_map:
                key, value = feature_map[param]
                result["features"][key] = value

        return result

    def _extract_pos(self) -> Optional[str]:
        """
        Extract part of speech from section headers.

        Common headers: ===Noun===, ===Verb===, ===Adjective===, etc.
        """
        # Normalize common POS
        pos_map = {
            'Noun': 'noun',
            'Verb': 'verb',
            'Adjective': 'adjective',
            'Adverb': 'adverb',
            'Pronoun': 'pronoun',
            'Preposition': 'preposition',
            'Conjunction': 'conjunction',
            'Interjection': 'interjection',
            'Particle': 'particle',
            'Determiner': 'determiner',
            'Article': 'article',
            'Numeral': 'numeral',
            'Proper noun': 'proper_noun',
            'Phrase': 'phrase',
        }

        # Find all level-3 headers
        headers = re.findall(r'^===\s*([^=\n]+?)\s*===', self.wikitext, re.MULTILINE)

        # Return the first one that matches a known POS
        for header in headers:
            header = header.strip()
            if header in pos_map:
                return pos_map[header]

        return None

    def _extract_etymology(self) -> Optional[str]:
        """
        Extract etymology section.

        Format: ===Etymology===
                Text content...
        """
        # Find etymology section
        match = re.search(
            r'===\s*Etymology\s*===\s*\n(.*?)(?=\n===|\Z)',
            self.wikitext,
            re.DOTALL
        )

        if match:
            etymology_text = match.group(1).strip()
            # Clean up wikitext markup
            etymology_text = self._clean_wikitext(etymology_text)
            return etymology_text if etymology_text else None

        return None

    def _extract_pronunciations(self) -> List[Dict[str, Any]]:
        """
        Extract pronunciation information.

        Looks for:
        - {{IPA|...}} templates
        - {{audio|...}} templates
        - Language-specific templates like {{es-pr|...}}
        """
        pronunciations = []

        # Find pronunciation section
        pron_section = re.search(
            r'===\s*Pronunciation\s*===\s*\n(.*?)(?=\n===|\Z)',
            self.wikitext,
            re.DOTALL
        )

        if not pron_section:
            return pronunciations

        pron_text = pron_section.group(1)

        # Extract IPA notation (multiple formats)
        # Format 1: {{IPA|lang|/ipa/|a=dialect}}
        ipa_matches = re.finditer(
            r'\{\{IPA\|[^|]*\|([^|}]+)(?:\|a=([^}]+))?\}\}',
            pron_text
        )

        for match in ipa_matches:
            ipa = match.group(1).strip()
            dialect = match.group(2).strip() if match.group(2) else None

            pronunciations.append({
                "ipa": ipa,
                "dialect": dialect,
                "audio_url": None
            })

        # Format 2: Direct IPA in text (e.g., /koˈreɾ/)
        # Look for IPA between forward slashes
        direct_ipa = re.findall(r'/([^/]+)/', pron_text)
        for ipa in direct_ipa:
            if ipa.strip() and not any(p['ipa'] == f'/{ipa}/' for p in pronunciations):
                pronunciations.append({
                    "ipa": f"/{ipa}/",
                    "dialect": None,
                    "audio_url": None
                })

        # Extract audio from various template formats
        # Format 1: {{audio|lang|filename|a=dialect}}
        # Format 2: <audio:filename<a:dialect>> (within language templates like {{es-pr}})

        # Match audio files in angle brackets (e.g., <audio:LL-Q1321 (spa)-file.wav<a:dialect>>)
        audio_matches = re.finditer(
            r'<audio:([^<>]+?\.(?:wav|ogg|mp3))(?:<a:([^<>]+)>)?',
            pron_text,
            re.IGNORECASE
        )

        for match in audio_matches:
            filename = match.group(1).strip()
            dialect = match.group(2).strip() if match.group(2) else None
            audio_url = f"https://commons.wikimedia.org/wiki/File:{filename}"

            # Try to match with existing pronunciation
            if pronunciations and not pronunciations[-1].get("audio_url"):
                pronunciations[-1]["audio_url"] = audio_url
                if dialect and not pronunciations[-1].get("dialect"):
                    pronunciations[-1]["dialect"] = dialect
            else:
                pronunciations.append({
                    "ipa": None,
                    "dialect": dialect,
                    "audio_url": audio_url
                })

        # Also match traditional {{audio|...}} templates
        audio_template_matches = re.finditer(
            r'\{\{audio\|[^|]*\|([^|<>]+?\.(?:wav|ogg|mp3))(?:\|a=([^}]+))?\}\}',
            pron_text,
            re.IGNORECASE
        )

        for match in audio_template_matches:
            filename = match.group(1).strip()
            dialect = match.group(2).strip() if match.group(2) else None
            audio_url = f"https://commons.wikimedia.org/wiki/File:{filename}"

            # Avoid duplicates
            if not any(p.get("audio_url") == audio_url for p in pronunciations):
                if pronunciations and not pronunciations[-1].get("audio_url"):
                    pronunciations[-1]["audio_url"] = audio_url
                    if dialect and not pronunciations[-1].get("dialect"):
                        pronunciations[-1]["dialect"] = dialect
                else:
                    pronunciations.append({
                        "ipa": None,
                        "dialect": dialect,
                        "audio_url": audio_url
                    })

        return pronunciations

    def _extract_definitions(self) -> List[Dict[str, Any]]:
        """
        Extract definitions.

        Format:
        # Definition 1
        #: {{syn|...}}
        #* Example sentence
        # Definition 2
        """
        definitions = []

        # Find the section after POS header
        # Definitions are marked with #
        lines = self.wikitext.split('\n')

        current_def = None
        def_number = 0

        in_definition_section = False

        for line in lines:
            line_stripped = line.strip()

            # Start tracking after we see a POS header
            if re.match(r'^===\s*[A-Z]', line):
                in_definition_section = True
                continue

            # Stop at next level-3 header (new section)
            if in_definition_section and re.match(r'^===', line) and current_def:
                definitions.append(current_def)
                break

            if not in_definition_section:
                continue

            # Main definition line (starts with #, not ## or #:, #*)
            if re.match(r'^#\s+[^:#*]', line):
                # Save previous definition (only if it has text)
                if current_def and current_def["text"]:
                    definitions.append(current_def)

                def_number += 1
                def_text = re.sub(r'^#\s+', '', line).strip()
                def_text = self._clean_wikitext(def_text)

                # Only create definition if there's actual text after cleaning
                if def_text:
                    current_def = {
                        "definition_number": def_number,
                        "text": def_text,
                        "examples": []
                    }
                else:
                    # Skip empty definitions (e.g., lines with only labels/templates)
                    current_def = None
                    def_number -= 1  # Don't increment counter for empty defs

            # Example (starts with #* or #:)
            elif current_def and re.match(r'^#[*:]\s+', line):
                example_text = re.sub(r'^#[*:]\s+', '', line).strip()
                example_text = self._clean_wikitext(example_text)

                if example_text:
                    current_def["examples"].append({
                        "text": example_text,
                        "translation": None  # TODO: extract translation if present
                    })

        # Add last definition (only if it has text)
        if current_def and current_def.get("text"):
            definitions.append(current_def)

        return definitions

    def _extract_word_forms(self) -> List[Dict[str, Any]]:
        """
        Extract word forms information from conjugation/declension templates.

        This extracts metadata about word forms rather than generating all forms.
        Actual inflected forms can be found as separate entries that reference
        back to the lemma.

        Returns:
            List of word form metadata dictionaries
        """
        word_forms = []

        # Find Conjugation or Declension sections
        conj_section = re.search(
            r'====\s*(?:Conjugation|Declension)\s*====\s*\n(.*?)(?=\n====|\n===|\Z)',
            self.wikitext,
            re.DOTALL
        )

        if not conj_section:
            return word_forms

        conj_text = conj_section.group(1)

        # Extract conjugation/declension templates
        # Common patterns:
        # {{es-conj}}
        # {{es-conj|correrse|nocomb=1}}
        # {{de-conj}} - verb conjugation
        # {{de-ndecl}} - noun declension
        # {{fr-conj}}

        template_matches = re.finditer(
            r'\{\{([a-z]{2,3}-(?:conj|decl|ndecl))(?:\|([^}]+))?\}\}',
            conj_text
        )

        for match in template_matches:
            template_name = match.group(1)
            template_params = match.group(2)

            form_info = {
                "template": template_name,
                "form_type": "conjugation" if "conj" in template_name else "declension"
            }

            # Extract explicit form from first parameter if present
            if template_params:
                # First parameter is often the word form (e.g., "correrse" for reflexive)
                params = [p.strip() for p in template_params.split('|')]
                if params and params[0] and '=' not in params[0]:
                    form_info["form"] = params[0]

                    # Detect if it's a reflexive form
                    if params[0].endswith('se') and self.language == "Spanish":
                        form_info["reflexive"] = True

            word_forms.append(form_info)

        # Also check for verb/noun templates in the POS section that might indicate forms
        # {{es-verb}} sometimes has parameters
        # Filter out common grammatical indicators
        grammatical_markers = {'m', 'f', 'n', 'c', 'p', 'sg', 'pl', 'mf', 'mfbysense'}

        pos_templates = re.finditer(
            r'\{\{([a-z]{2,3}-(verb|noun|adj))(?:\|([^}]+))?\}\}',
            self.wikitext
        )

        for match in pos_templates:
            template_params = match.group(3)
            if template_params:
                # Get first parameter only (split by |)
                first_param = template_params.split('|')[0].strip()

                # Only consider if it's a valid word form (not a single letter grammar marker)
                if (first_param and
                    '=' not in first_param and
                    first_param not in grammatical_markers and
                    len(first_param) > 2):  # Longer than 2 chars to avoid markers

                    # Only add if not already in word_forms
                    if not any(wf.get("form") == first_param for wf in word_forms):
                        word_forms.append({
                            "form": first_param,
                            "form_type": "variant"
                        })

        return word_forms

    def _clean_wikitext(self, text: str) -> str:
        """
        Clean wikitext markup from text.

        Removes:
        - Wiki links [[link|text]] -> text
        - Templates {{template|...}} -> remove or simplify
        - Bold/italic markup
        - etc.
        """
        # Remove references <ref>...</ref>
        text = re.sub(r'<ref[^>]*>.*?</ref>', '', text, flags=re.DOTALL)

        # Remove metadata templates that don't contain content
        # These should be removed entirely
        metadata_templates = [
            'syn', 'ant', 'cog', 'der', 'inh', 'bor', 'lb', 'l', 'link',
            'gloss', 'qualifier', 'q', 'topics', 'categorize', 'cln',
            'c', 'C', 'trans-top', 'trans-bottom', 'trans-mid',
            'see', 'seeCites', 'RQ:', 'cite', 'quote-book', 'quote-journal',
            'ux', 'uxi', 'usex', 'afex'  # Usage examples have special handling
        ]

        # Remove these templates entirely
        for template in metadata_templates:
            text = re.sub(
                rf'\{{\{{' + re.escape(template) + r'\|[^}}]*\}\}',
                '',
                text
            )

        # Convert wiki links [[target|display]] -> display, [[target]] -> target
        text = re.sub(r'\[\[([^|\]]+)\|([^\]]+)\]\]', r'\2', text)  # [[a|b]] -> b
        text = re.sub(r'\[\[([^\]]+)\]\]', r'\1', text)  # [[a]] -> a

        # Remove bold/italic
        text = re.sub(r"'{2,}", '', text)

        # Expand form-of templates BEFORE general template cleanup
        # These templates need special handling to preserve the referenced word
        # Format: {{plural of|lang|word}} -> "plural of word"
        # Format: {{feminine of|lang|word}} -> "feminine of word"
        def expand_form_of(match):
            template = match.group(0)
            # Remove {{ and }}
            template_clean = template.strip('{}')
            parts = [p.strip() for p in template_clean.split('|')]

            if len(parts) < 2:
                return match.group(1) if len(match.groups()) > 0 else template

            template_name = parts[0]

            # Common form-of templates
            # Usually format is: {{template|lang|word}} or {{template|word}}
            # We want to extract the word (last meaningful parameter)

            # Find the target word (skip language code if present)
            target_word = None
            for i in range(1, len(parts)):
                part = parts[i].strip()
                # Skip language codes (2-3 letter codes)
                if len(part) > 3 or i == len(parts) - 1:
                    target_word = part
                    break

            if target_word:
                return f"{template_name} {target_word}"
            else:
                return template_name

        # Apply to common form-of templates
        form_of_templates = [
            'plural of', 'feminine of', 'masculine of', 'singular of',
            'past of', 'present of', 'past tense of', 'past participle of',
            'gerund of', 'present participle of', 'comparative of', 'superlative of',
            'diminutive of', 'augmentative of', 'alternative form of', 'archaic form of',
            'obsolete form of', 'inflection of', 'conjugation of'
        ]

        for template in form_of_templates:
            # Match templates with this name
            pattern = r'\{\{' + re.escape(template) + r'(\|[^}]+)?\}\}'
            text = re.sub(pattern, expand_form_of, text)

        # Remove remaining templates (conservative - just remove the template syntax)
        text = re.sub(r'\{\{([^}|]+)(\|[^}]*)?\}\}', r'\1', text)

        # Clean up extra whitespace
        text = re.sub(r'\s+', ' ', text).strip()

        return text


def parse_entry(language: str, lemma: str, wikitext: str) -> List[Dict[str, Any]]:
    """
    Parse a single language section from a Wiktionary entry.

    Args:
        language: Language name (e.g., "Spanish")
        lemma: Word/lemma (e.g., "correr")
        wikitext: Wikitext content for this language section

    Returns:
        List of structured dictionaries (one per POS section for lemmas)
    """
    parser = WikitextParser(language, lemma, wikitext)
    return parser.parse()


def main():
    """
    Test the parser with a sample entry.
    """
    import sys

    if len(sys.argv) < 2:
        print("Usage: python wikitext_parser.py <word>")
        print("Example: python wikitext_parser.py gratis")
        sys.exit(1)

    word = sys.argv[1]

    # Use query_entry to get the raw XML
    import subprocess
    result = subprocess.run(
        ['python3', 'query_entry.py', word],
        capture_output=True,
        text=True
    )

    if result.returncode != 0:
        print(f"Error: Could not fetch word '{word}'")
        sys.exit(1)

    # Extract Spanish section from XML
    # TODO: Parse XML properly, extract language section
    print(f"Fetched entry for: {word}")
    print("TODO: Parse and display structured output")


if __name__ == "__main__":
    main()
