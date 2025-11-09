#!/usr/bin/env python3
"""
Import reading texts from JSON files into Vocabee database via API.

Usage:
    python3 import_reading_texts.py [directory]
    python3 import_reading_texts.py texts/generated/fr

If no directory is provided, searches in texts/ and all subdirectories.
"""

import json
import os
import sys
import requests
from pathlib import Path
from typing import Dict, List

API_BASE = "http://localhost:8080/api/reading"

def find_json_files(directory: str) -> List[Path]:
    """Find all JSON files in directory and subdirectories."""
    path = Path(directory)
    json_files = []

    if path.is_file() and path.suffix == '.json':
        return [path]

    for json_file in path.rglob('*.json'):
        # Skip template and README files
        if json_file.name.lower() in ['template.json', 'readme.json', 'index.json']:
            continue
        json_files.append(json_file)

    return sorted(json_files)

def load_text_from_json(filepath: Path) -> Dict:
    """Load and validate text data from JSON file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # Validate required fields
    required_fields = ['title', 'content', 'language_code']
    for field in required_fields:
        if field not in data:
            raise ValueError(f"Missing required field: {field}")

    return data

def import_text(text_data: Dict, auth_token: str = None) -> Dict:
    """Import a text via API."""
    headers = {
        'Content-Type': 'application/json'
    }

    if auth_token:
        headers['Authorization'] = f'Bearer {auth_token}'

    # Convert snake_case to camelCase for API
    request_data = {
        'title': text_data['title'],
        'content': text_data['content'],
        'languageCode': text_data['language_code'],
        'level': text_data.get('level'),
        'topic': text_data.get('topic'),
        'description': text_data.get('description'),
        'author': text_data.get('author', 'import'),
        'source': text_data.get('source', 'import')
    }

    response = requests.post(
        f'{API_BASE}/texts/import',
        headers=headers,
        json=request_data
    )

    response.raise_for_status()
    return response.json()

def main():
    directory = sys.argv[1] if len(sys.argv) > 1 else 'texts'

    print(f"Searching for JSON files in: {directory}")
    json_files = find_json_files(directory)

    if not json_files:
        print(f"No JSON files found in {directory}")
        return

    print(f"Found {len(json_files)} JSON files to import\n")

    # Optional: Get auth token from environment or user
    auth_token = os.environ.get('VOCABEE_AUTH_TOKEN')

    imported = 0
    failed = 0

    for json_file in json_files:
        try:
            print(f"Importing: {json_file.name}...", end=' ')
            text_data = load_text_from_json(json_file)
            result = import_text(text_data, auth_token)

            print(f"✓ Imported as ID {result['id']}: {result['title']}")
            imported += 1

        except Exception as e:
            print(f"✗ Failed: {str(e)}")
            failed += 1

    print(f"\n{'='*60}")
    print(f"Import complete!")
    print(f"  Imported: {imported}")
    print(f"  Failed:   {failed}")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()
