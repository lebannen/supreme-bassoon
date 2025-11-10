#!/usr/bin/env python3
"""
Batch Audio Generator for Reading Texts

Generates speech audio for all text files in the texts/generated directory.
Handles rate limiting, progress tracking, and error recovery.

Usage:
    python batch_generate_audio.py [options]

Examples:
    # Process all texts with default settings
    python batch_generate_audio.py

    # Specify custom directories
    python batch_generate_audio.py --input texts/generated --output audio_output

    # Use specific model and voice
    python batch_generate_audio.py --model gemini-2.5-pro-preview-tts

    # Resume from previous run
    python batch_generate_audio.py --resume

    # Dry run (show what would be processed)
    python batch_generate_audio.py --dry-run
"""

import argparse
import json
import logging
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

from gemini_tts import GeminiTTS


# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%H:%M:%S'
)
logger = logging.getLogger(__name__)


class BatchAudioGenerator:
    """Batch processor for generating audio from text files."""

    def __init__(
        self,
        input_dir: str = "texts/generated",
        output_dir: str = "audio_generated",
        progress_file: str = "audio_generation_progress.json",
        rate_limit_seconds: float = 20.0,
        model: str = "gemini-2.5-flash-preview-tts",
        api_key: Optional[str] = None
    ):
        """
        Initialize batch generator.

        Args:
            input_dir: Directory containing text JSON files
            output_dir: Directory for output audio files
            progress_file: JSON file to track progress
            rate_limit_seconds: Seconds to wait between API calls (default: 20s for 3 RPM)
            model: Gemini model to use
            api_key: API key (optional, uses env var if not provided)
        """
        self.input_dir = Path(input_dir)
        self.output_dir = Path(output_dir)
        self.progress_file = Path(progress_file)
        self.rate_limit_seconds = rate_limit_seconds
        self.model = model

        # Initialize TTS client
        self.tts = GeminiTTS(api_key=api_key, model=model)

        # Progress tracking
        self.progress = self._load_progress()
        self.stats = {
            "total": 0,
            "processed": 0,
            "skipped": 0,
            "failed": 0,
            "start_time": None,
            "end_time": None
        }

    def _load_progress(self) -> Dict:
        """Load progress from JSON file."""
        if self.progress_file.exists():
            with open(self.progress_file, 'r') as f:
                return json.load(f)
        return {"completed": [], "failed": []}

    def _save_progress(self):
        """Save progress to JSON file."""
        with open(self.progress_file, 'w') as f:
            json.dump(self.progress, f, indent=2)

    def _find_text_files(self) -> List[Path]:
        """Find all text JSON files in input directory."""
        if not self.input_dir.exists():
            raise FileNotFoundError(f"Input directory not found: {self.input_dir}")

        files = list(self.input_dir.rglob("*.json"))
        logger.info(f"Found {len(files)} text files in {self.input_dir}")
        return files

    def _load_text_metadata(self, file_path: Path) -> Optional[Dict]:
        """Load and parse text JSON file."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)

            # Validate required fields
            required = ['content', 'language_code']
            if not all(field in data for field in required):
                logger.warning(f"Missing required fields in {file_path.name}")
                return None

            return data
        except Exception as e:
            logger.error(f"Error loading {file_path.name}: {e}")
            return None

    def _generate_output_path(self, text_file: Path, language_code: str) -> Path:
        """Generate output path for audio file."""
        # Preserve directory structure: audio_generated/fr/fr_a1_bakery.wav
        relative_path = text_file.relative_to(self.input_dir)
        output_path = self.output_dir / relative_path.parent / f"{text_file.stem}.wav"
        return output_path

    def process_file(self, text_file: Path, dry_run: bool = False) -> str:
        """
        Process a single text file.

        Args:
            text_file: Path to text JSON file
            dry_run: If True, only simulate processing

        Returns:
            "processed" if successfully generated audio
            "skipped" if already processed
            "failed" if error occurred
        """
        file_id = str(text_file.relative_to(self.input_dir))

        # Check if already processed
        if file_id in self.progress["completed"]:
            logger.info(f"⏭️  Skipping (already processed): {file_id}")
            self.stats["skipped"] += 1
            return "skipped"

        # Load metadata
        metadata = self._load_text_metadata(text_file)
        if not metadata:
            self.stats["failed"] += 1
            return "failed"

        # Prepare paths
        output_path = self._generate_output_path(text_file, metadata['language_code'])

        if dry_run:
            logger.info(f"🔍 Would process: {file_id}")
            logger.info(f"   Output: {output_path}")
            logger.info(f"   Language: {metadata['language_code']}")
            logger.info(f"   Text length: {len(metadata['content'])} chars")
            return "processed"

        # Generate audio
        logger.info(f"🎙️  Processing ({self.stats['processed'] + 1}/{self.stats['total']}): {file_id}")
        logger.info(f"   Language: {metadata['language_code']}")
        logger.info(f"   Text length: {len(metadata['content'])} chars")

        try:
            result_path = self.tts.generate_speech(
                text=metadata['content'],
                output_path=str(output_path),
                language_code=metadata['language_code']
            )

            file_size = Path(result_path).stat().st_size / 1024 / 1024  # MB
            logger.info(f"   ✅ Saved: {result_path} ({file_size:.2f} MB)")

            # Update progress
            self.progress["completed"].append(file_id)
            self._save_progress()
            self.stats["processed"] += 1

            return "processed"

        except Exception as e:
            logger.error(f"   ❌ Failed: {e}")
            self.progress["failed"].append({
                "file": file_id,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            })
            self._save_progress()
            self.stats["failed"] += 1
            return "failed"

    def run(self, dry_run: bool = False, resume: bool = True):
        """
        Run batch processing.

        Args:
            dry_run: If True, only show what would be processed
            resume: If True, skip already processed files
        """
        logger.info("=" * 60)
        logger.info("🎵 Batch Audio Generation Starting")
        logger.info("=" * 60)

        # Find files
        text_files = self._find_text_files()
        self.stats["total"] = len(text_files)

        if self.stats["total"] == 0:
            logger.warning("No text files found!")
            return

        # Show configuration
        logger.info(f"Input directory: {self.input_dir}")
        logger.info(f"Output directory: {self.output_dir}")
        logger.info(f"Model: {self.model}")
        logger.info(f"Rate limit: {self.rate_limit_seconds}s between requests")
        logger.info(f"Resume mode: {'enabled' if resume else 'disabled'}")

        if not resume:
            self.progress = {"completed": [], "failed": []}
            self._save_progress()
            logger.info("Progress reset (resume disabled)")

        if dry_run:
            logger.info("\n🔍 DRY RUN MODE - No audio will be generated\n")

        # Estimate time
        files_to_process = len([f for f in text_files
                                if str(f.relative_to(self.input_dir)) not in self.progress["completed"]])
        estimated_minutes = (files_to_process * self.rate_limit_seconds) / 60

        logger.info(f"\nFiles to process: {files_to_process}/{self.stats['total']}")
        logger.info(f"Estimated time: {estimated_minutes:.1f} minutes\n")

        # Process files
        self.stats["start_time"] = datetime.now()
        last_was_processed = False

        for text_file in text_files:
            # Rate limiting (only after actual API calls, not skipped files)
            if last_was_processed and not dry_run:
                logger.info(f"⏳ Rate limit: waiting {self.rate_limit_seconds}s...")
                time.sleep(self.rate_limit_seconds)

            # Process file
            result = self.process_file(text_file, dry_run=dry_run)
            last_was_processed = (result == "processed")

        self.stats["end_time"] = datetime.now()

        # Summary
        self._print_summary()

    def _print_summary(self):
        """Print processing summary."""
        logger.info("\n" + "=" * 60)
        logger.info("📊 Processing Summary")
        logger.info("=" * 60)
        logger.info(f"Total files: {self.stats['total']}")
        logger.info(f"Processed: {self.stats['processed']}")
        logger.info(f"Skipped (already done): {self.stats['skipped']}")
        logger.info(f"Failed: {self.stats['failed']}")

        if self.stats["start_time"] and self.stats["end_time"]:
            duration = (self.stats["end_time"] - self.stats["start_time"]).total_seconds()
            logger.info(f"Duration: {duration / 60:.1f} minutes")

        if self.stats["failed"] > 0:
            logger.warning(f"\n⚠️  {self.stats['failed']} files failed. Check {self.progress_file} for details.")

        logger.info("=" * 60)


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="Batch generate speech audio for reading texts",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    parser.add_argument(
        "--input",
        default="texts/generated",
        help="Input directory containing text JSON files (default: texts/generated)"
    )

    parser.add_argument(
        "--output",
        default="audio_generated",
        help="Output directory for audio files (default: audio_generated)"
    )

    parser.add_argument(
        "--model",
        default="gemini-2.5-flash-preview-tts",
        help="Gemini model to use (default: gemini-2.5-flash-preview-tts)"
    )

    parser.add_argument(
        "--rate-limit",
        type=float,
        default=20.0,
        help="Seconds between API requests (default: 20 for 3 RPM)"
    )

    parser.add_argument(
        "--progress-file",
        default="audio_generation_progress.json",
        help="Progress tracking file (default: audio_generation_progress.json)"
    )

    parser.add_argument(
        "--resume",
        action="store_true",
        default=True,
        help="Resume from previous run (default: True)"
    )

    parser.add_argument(
        "--no-resume",
        action="store_true",
        help="Start fresh (ignore previous progress)"
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be processed without generating audio"
    )

    args = parser.parse_args()

    # Create generator
    generator = BatchAudioGenerator(
        input_dir=args.input,
        output_dir=args.output,
        progress_file=args.progress_file,
        rate_limit_seconds=args.rate_limit,
        model=args.model
    )

    # Run
    generator.run(
        dry_run=args.dry_run,
        resume=not args.no_resume
    )


if __name__ == "__main__":
    main()
