#!/usr/bin/env python3
"""
Upload generated audio files to MinIO and update database with audio URLs.

This script:
1. Scans audio_generated/ directory for .wav files
2. Uploads each file to MinIO (S3-compatible storage)
3. Updates the corresponding reading text in the database with the audio URL

Requirements:
    pip install boto3 requests

Usage:
    python upload_audio_to_storage.py [--dry-run] [--backend-url http://localhost:8080]
"""

import os
import sys
import json
import argparse
from pathlib import Path
from typing import Dict, List, Optional
import requests
import boto3
from botocore.client import Config

class AudioUploader:
    def __init__(
        self,
        s3_endpoint: str = "http://localhost:9000",
        s3_access_key: str = "minioadmin",
        s3_secret_key: str = "minioadmin123",
        bucket_name: str = "vocabee-audio",
        backend_url: str = "http://localhost:8080"
    ):
        """Initialize the audio uploader with S3 and backend configuration."""
        self.bucket_name = bucket_name
        self.backend_url = backend_url

        # Initialize S3 client for MinIO
        self.s3_client = boto3.client(
            's3',
            endpoint_url=s3_endpoint,
            aws_access_key_id=s3_access_key,
            aws_secret_access_key=s3_secret_key,
            config=Config(signature_version='s3v4'),
            region_name='us-east-1'
        )

        print(f"✓ Connected to MinIO at {s3_endpoint}")
        print(f"✓ Backend API at {backend_url}")

    def get_reading_texts(self) -> List[Dict]:
        """Fetch all reading texts from the backend API."""
        try:
            response = requests.get(f"{self.backend_url}/api/reading/texts")
            response.raise_for_status()
            texts = response.json()
            print(f"✓ Fetched {len(texts)} reading texts from backend")
            return texts
        except requests.exceptions.RequestException as e:
            print(f"✗ Failed to fetch reading texts: {e}")
            sys.exit(1)

    def find_text_by_json_filename(self, texts: List[Dict], json_filename: str) -> Optional[Dict]:
        """
        Find a reading text that matches the JSON filename.

        The JSON filename format is: {lang}_{level}_{topic}.json
        For example: fr_a1_bakery.json
        """
        # Extract components from filename
        parts = json_filename.replace('.json', '').split('_')
        if len(parts) < 3:
            return None

        lang = parts[0]
        level = parts[1].upper()
        topic = '_'.join(parts[2:])

        # Search for matching text
        for text in texts:
            text_lang = text.get('languageCode', '')
            text_level = text.get('level', '').upper()
            text_topic = text.get('topic', '')

            if (text_lang == lang and
                text_level == level and
                text_topic.replace(' ', '_').lower() == topic):
                return text

        return None

    def upload_audio_file(self, file_path: Path) -> str:
        """
        Upload an audio file to MinIO and return the public URL.

        Args:
            file_path: Path to the audio file

        Returns:
            Public URL of the uploaded file
        """
        # Generate S3 key (filename in bucket)
        s3_key = f"tts/{file_path.name}"

        # Upload file
        with open(file_path, 'rb') as f:
            self.s3_client.put_object(
                Bucket=self.bucket_name,
                Key=s3_key,
                Body=f,
                ContentType='audio/wav'
            )

        # Generate public URL
        url = f"http://localhost:9000/{self.bucket_name}/{s3_key}"
        return url

    def update_text_audio_url(self, text_id: int, audio_url: str) -> bool:
        """
        Update a reading text with the audio URL via backend API.

        Args:
            text_id: ID of the reading text
            audio_url: URL of the audio file

        Returns:
            True if successful, False otherwise
        """
        try:
            response = requests.patch(
                f"{self.backend_url}/api/reading/texts/{text_id}/audio",
                json={"audioUrl": audio_url},
                headers={"Content-Type": "application/json"}
            )
            response.raise_for_status()
            return True
        except requests.exceptions.RequestException as e:
            print(f"✗ Failed to update text {text_id}: {e}")
            return False

    def process_audio_files(self, audio_dir: Path, dry_run: bool = False):
        """
        Process all audio files in the directory.

        Args:
            audio_dir: Directory containing audio files
            dry_run: If True, don't actually upload or update
        """
        # Find all audio files (recursively in subdirectories)
        audio_files = list(audio_dir.glob("**/*.wav"))

        if not audio_files:
            print(f"No audio files found in {audio_dir}")
            return

        print(f"\nFound {len(audio_files)} audio files")

        # Fetch all reading texts
        texts = self.get_reading_texts()

        # Process each audio file
        results = {
            'uploaded': [],
            'skipped': [],
            'failed': []
        }

        for audio_file in sorted(audio_files):
            print(f"\n{'[DRY RUN] ' if dry_run else ''}Processing: {audio_file.name}")

            # Find corresponding text in database
            json_filename = audio_file.stem + '.json'
            text = self.find_text_by_json_filename(texts, json_filename)

            if not text:
                print(f"  ✗ No matching text found for {json_filename}")
                results['skipped'].append(audio_file.name)
                continue

            text_id = text['id']
            text_title = text['title']

            # Check if text already has audio
            if text.get('audioUrl'):
                print(f"  ⊙ Text '{text_title}' (ID: {text_id}) already has audio: {text['audioUrl']}")
                results['skipped'].append(audio_file.name)
                continue

            print(f"  → Found matching text: '{text_title}' (ID: {text_id})")

            if dry_run:
                print(f"  → Would upload to MinIO and update database")
                results['uploaded'].append(audio_file.name)
                continue

            try:
                # Upload to MinIO
                print(f"  → Uploading to MinIO...")
                audio_url = self.upload_audio_file(audio_file)
                print(f"  ✓ Uploaded: {audio_url}")

                # Update database
                print(f"  → Updating database...")
                if self.update_text_audio_url(text_id, audio_url):
                    print(f"  ✓ Updated text {text_id} with audio URL")
                    results['uploaded'].append(audio_file.name)
                else:
                    results['failed'].append(audio_file.name)

            except Exception as e:
                print(f"  ✗ Error processing {audio_file.name}: {e}")
                results['failed'].append(audio_file.name)

        # Print summary
        print("\n" + "=" * 60)
        print("SUMMARY")
        print("=" * 60)
        print(f"Total files:    {len(audio_files)}")
        print(f"Uploaded:       {len(results['uploaded'])}")
        print(f"Skipped:        {len(results['skipped'])}")
        print(f"Failed:         {len(results['failed'])}")

        if results['failed']:
            print("\nFailed files:")
            for filename in results['failed']:
                print(f"  - {filename}")


def main():
    parser = argparse.ArgumentParser(
        description="Upload audio files to MinIO and update database"
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Simulate the upload without making changes'
    )
    parser.add_argument(
        '--backend-url',
        default='http://localhost:8080',
        help='Backend API URL (default: http://localhost:8080)'
    )
    parser.add_argument(
        '--s3-endpoint',
        default='http://localhost:9000',
        help='S3 endpoint URL (default: http://localhost:9000)'
    )
    parser.add_argument(
        '--audio-dir',
        type=Path,
        default=Path(__file__).parent.parent.parent / 'audio_generated',
        help='Directory containing audio files'
    )

    args = parser.parse_args()

    # Check if audio directory exists
    if not args.audio_dir.exists():
        print(f"Error: Audio directory not found: {args.audio_dir}")
        sys.exit(1)

    print("=" * 60)
    print("AUDIO UPLOAD TO MINIO")
    print("=" * 60)

    if args.dry_run:
        print("🔍 DRY RUN MODE - No changes will be made")

    print(f"Audio directory: {args.audio_dir}")
    print(f"Backend URL:     {args.backend_url}")
    print(f"S3 Endpoint:     {args.s3_endpoint}")
    print()

    # Create uploader and process files
    uploader = AudioUploader(
        s3_endpoint=args.s3_endpoint,
        backend_url=args.backend_url
    )

    uploader.process_audio_files(args.audio_dir, dry_run=args.dry_run)


if __name__ == '__main__':
    main()
