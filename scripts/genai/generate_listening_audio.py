#!/usr/bin/env python3
"""
Generate audio files for listening exercises.

This script reads the listening exercise data and generates audio files
for each exercise transcript using Gemini TTS.

Usage:
    python generate_listening_audio.py
"""

import json
import logging
import time
from pathlib import Path
from typing import List, Dict

from gemini_tts import GeminiTTS

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%H:%M:%S'
)
logger = logging.getLogger(__name__)

# Listening exercise data
EXERCISES = [
    {
        "id": "bonjour",
        "language_code": "fr",
        "transcript": "Bonjour",
        "output_path": "audio/fr/greetings/bonjour.mp3"
    },
    {
        "id": "cinq",
        "language_code": "fr",
        "transcript": "Le nombre est cinq",
        "output_path": "audio/fr/numbers/cinq.mp3"
    },
    {
        "id": "comment_allez_vous",
        "language_code": "fr",
        "transcript": "Comment allez-vous?",
        "output_path": "audio/fr/phrases/comment_allez_vous.mp3"
    },
    {
        "id": "lundi",
        "language_code": "fr",
        "transcript": "lundi",
        "output_path": "audio/fr/days/lundi.mp3"
    },
    {
        "id": "le_pain",
        "language_code": "fr",
        "transcript": "le pain",
        "output_path": "audio/fr/food/le_pain.mp3"
    },
    {
        "id": "quel_age",
        "language_code": "fr",
        "transcript": "Quel âge avez-vous?",
        "output_path": "audio/fr/questions/quel_age.mp3"
    }
]


class ListeningAudioGenerator:
    """Generator for listening exercise audio files."""

    def __init__(
        self,
        output_base_dir: str = "../../frontend/public",
        rate_limit_seconds: float = 20.0,
        model: str = "gemini-2.5-flash-preview-tts"
    ):
        """
        Initialize generator.

        Args:
            output_base_dir: Base directory for output files
            rate_limit_seconds: Seconds to wait between API calls
            model: Gemini model to use
        """
        self.output_base_dir = Path(output_base_dir)
        self.rate_limit_seconds = rate_limit_seconds
        self.tts = GeminiTTS(model=model)
        self.stats = {
            "total": 0,
            "processed": 0,
            "failed": 0
        }

    def generate_audio(self, exercise: Dict) -> bool:
        """
        Generate audio for a single exercise.

        Args:
            exercise: Exercise data dict

        Returns:
            True if successful, False otherwise
        """
        output_path = self.output_base_dir / exercise["output_path"]

        # Skip if already exists
        if output_path.exists():
            logger.info(f"⏭️  Skipping (already exists): {exercise['id']}")
            return True

        logger.info(f"🎙️  Generating ({self.stats['processed'] + 1}/{self.stats['total']}): {exercise['id']}")
        logger.info(f"   Transcript: {exercise['transcript']}")
        logger.info(f"   Language: {exercise['language_code']}")
        logger.info(f"   Output: {output_path}")

        try:
            result_path = self.tts.generate_speech(
                text=exercise['transcript'],
                output_path=str(output_path),
                language_code=exercise['language_code']
            )

            file_size = Path(result_path).stat().st_size / 1024  # KB
            logger.info(f"   ✅ Saved: {result_path} ({file_size:.1f} KB)")
            self.stats["processed"] += 1
            return True

        except Exception as e:
            logger.error(f"   ❌ Failed: {e}")
            self.stats["failed"] += 1
            return False

    def run(self):
        """Run audio generation for all exercises."""
        logger.info("=" * 60)
        logger.info("🎵 Listening Exercise Audio Generation")
        logger.info("=" * 60)

        self.stats["total"] = len(EXERCISES)
        logger.info(f"Total exercises: {self.stats['total']}")
        logger.info(f"Output directory: {self.output_base_dir}")
        logger.info(f"Rate limit: {self.rate_limit_seconds}s between requests")

        estimated_minutes = (self.stats["total"] * self.rate_limit_seconds) / 60
        logger.info(f"Estimated time: {estimated_minutes:.1f} minutes\n")

        # Generate audio for each exercise
        for i, exercise in enumerate(EXERCISES):
            # Rate limiting (skip for first request)
            if i > 0:
                logger.info(f"⏳ Rate limit: waiting {self.rate_limit_seconds}s...")
                time.sleep(self.rate_limit_seconds)

            self.generate_audio(exercise)

        # Summary
        self._print_summary()

    def _print_summary(self):
        """Print processing summary."""
        logger.info("\n" + "=" * 60)
        logger.info("📊 Processing Summary")
        logger.info("=" * 60)
        logger.info(f"Total exercises: {self.stats['total']}")
        logger.info(f"Processed: {self.stats['processed']}")
        logger.info(f"Failed: {self.stats['failed']}")

        if self.stats["failed"] > 0:
            logger.warning(f"\n⚠️  {self.stats['failed']} exercises failed.")
        else:
            logger.info("\n✅ All audio files generated successfully!")

        logger.info("=" * 60)


def main():
    """Main entry point."""
    generator = ListeningAudioGenerator()
    generator.run()


if __name__ == "__main__":
    main()
