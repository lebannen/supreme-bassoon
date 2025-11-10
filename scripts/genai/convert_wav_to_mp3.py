#!/usr/bin/env python3
"""
Convert existing WAV files to MP3 format (128kbps)
"""

from pathlib import Path
from pydub import AudioSegment

def convert_wav_to_mp3(wav_path: Path, mp3_path: Path = None) -> Path:
    """
    Convert a WAV file to MP3 at 128kbps.

    Args:
        wav_path: Path to the WAV file
        mp3_path: Optional output path (defaults to same location with .mp3 extension)

    Returns:
        Path to the created MP3 file
    """
    if mp3_path is None:
        mp3_path = wav_path.with_suffix('.mp3')

    print(f"Converting {wav_path.name}...", end=' ')

    # Load WAV file
    audio = AudioSegment.from_wav(str(wav_path))

    # Export as MP3 with 128kbps bitrate
    audio.export(
        str(mp3_path),
        format="mp3",
        bitrate="128k"
    )

    # Get file sizes for comparison
    wav_size = wav_path.stat().st_size / (1024 * 1024)  # MB
    mp3_size = mp3_path.stat().st_size / (1024 * 1024)  # MB
    savings = ((wav_size - mp3_size) / wav_size) * 100

    print(f"✓ ({wav_size:.2f}MB → {mp3_size:.2f}MB, {savings:.0f}% smaller)")

    return mp3_path


def main():
    """Convert all WAV files in audio_generated directory."""
    base_dir = Path(__file__).parent.parent.parent / "audio_generated"

    if not base_dir.exists():
        print(f"Error: Directory not found: {base_dir}")
        return

    # Find all WAV files
    wav_files = list(base_dir.glob("**/*.wav"))

    if not wav_files:
        print(f"No WAV files found in {base_dir}")
        return

    print(f"Found {len(wav_files)} WAV files to convert\n")

    total_wav_size = 0
    total_mp3_size = 0

    for wav_path in wav_files:
        mp3_path = convert_wav_to_mp3(wav_path)

        total_wav_size += wav_path.stat().st_size
        total_mp3_size += mp3_path.stat().st_size

    # Summary
    total_wav_mb = total_wav_size / (1024 * 1024)
    total_mp3_mb = total_mp3_size / (1024 * 1024)
    total_savings = ((total_wav_size - total_mp3_size) / total_wav_size) * 100

    print(f"\n{'='*60}")
    print(f"Conversion complete!")
    print(f"{'='*60}")
    print(f"Total WAV size: {total_wav_mb:.2f} MB")
    print(f"Total MP3 size: {total_mp3_mb:.2f} MB")
    print(f"Total savings:  {total_savings:.0f}% ({total_wav_mb - total_mp3_mb:.2f} MB)")
    print(f"\nYou can now delete the WAV files if desired:")
    print(f"  rm audio_generated/**/*.wav")


if __name__ == "__main__":
    main()
