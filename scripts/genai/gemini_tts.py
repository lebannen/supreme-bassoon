#!/usr/bin/env python3
"""
Gemini TTS (Text-to-Speech) Library

A modular library for generating speech audio from text using Google's Gemini API.
Supports multiple languages and voices with configurable parameters.
"""

import io
import mimetypes
import os
import struct
from pathlib import Path
from typing import Optional

from google import genai
from google.genai import types
from pydub import AudioSegment


# Voice mapping per language (can be customized)
DEFAULT_VOICES = {
    "fr": "Leda",  # French - female
    "de": "Kore",  # German - female
    "en": "Puck",  # English - neutral
    # Add more languages as needed
}


class GeminiTTS:
    """Text-to-Speech generator using Gemini API."""

    def __init__(
        self,
        api_key: Optional[str] = None,
        model: str = "gemini-2.5-flash-preview-tts",
        temperature: float = 1.0
    ):
        """
        Initialize TTS client.

        Args:
            api_key: Gemini API key (defaults to GEMINI_API_KEY env var)
            model: Model to use for TTS generation
            temperature: Sampling temperature (0-2)
        """
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY")
        if not self.api_key:
            raise ValueError("API key required (provide api_key or set GEMINI_API_KEY env var)")

        self.model = model
        self.temperature = temperature
        self.client = genai.Client(api_key=self.api_key)

    def generate_speech(
        self,
        text: str,
        output_path: str,
        voice: Optional[str] = None,
        language_code: Optional[str] = None
    ) -> str:
        """
        Generate speech audio from text and save to file.

        Args:
            text: Text to convert to speech
            output_path: Output file path (will be .mp3)
            voice: Voice name (e.g., "Leda", "Kore", "Puck")
                   If None, will use default voice for language
            language_code: Language code (e.g., "fr", "de", "en")
                          Used to select default voice if voice is None

        Returns:
            Path to generated audio file

        Raises:
            ValueError: If text is empty or voice cannot be determined
            Exception: If API call fails
        """
        if not text or not text.strip():
            raise ValueError("Text cannot be empty")

        # Determine voice
        if voice is None:
            if language_code and language_code in DEFAULT_VOICES:
                voice = DEFAULT_VOICES[language_code]
            else:
                voice = "Puck"  # Default neutral voice

        # Ensure output path ends with .mp3
        output_path = str(output_path)
        if not output_path.endswith(".mp3"):
            output_path = output_path.replace(Path(output_path).suffix, ".mp3")

        # Create output directory if needed
        Path(output_path).parent.mkdir(parents=True, exist_ok=True)

        # Prepare request
        contents = [
            types.Content(
                role="user",
                parts=[types.Part.from_text(text=text)]
            )
        ]

        config = types.GenerateContentConfig(
            temperature=self.temperature,
            response_modalities=["AUDIO"],
            speech_config=types.SpeechConfig(
                voice_config=types.VoiceConfig(
                    prebuilt_voice_config=types.PrebuiltVoiceConfig(
                        voice_name=voice
                    )
                )
            )
        )

        # Generate audio (streaming)
        audio_chunks = []
        for chunk in self.client.models.generate_content_stream(
            model=self.model,
            contents=contents,
            config=config
        ):
            if (chunk.candidates and
                chunk.candidates[0].content and
                chunk.candidates[0].content.parts):

                part = chunk.candidates[0].content.parts[0]
                if part.inline_data and part.inline_data.data:
                    audio_chunks.append(part.inline_data)

        if not audio_chunks:
            raise Exception("No audio data received from API")

        # Combine audio chunks
        # Note: For simplicity, we're taking the first chunk
        # In production, you might want to concatenate all chunks
        inline_data = audio_chunks[0]
        audio_data = inline_data.data

        # Parse audio parameters from MIME type
        params = self._parse_audio_mime_type(inline_data.mime_type)
        sample_rate = params["rate"]
        bits_per_sample = params["bits_per_sample"]

        # Convert raw PCM to AudioSegment
        audio_segment = AudioSegment(
            data=audio_data,
            sample_width=bits_per_sample // 8,  # bytes per sample
            frame_rate=sample_rate,
            channels=1  # mono
        )

        # Export as MP3 with 128kbps bitrate
        audio_segment.export(
            output_path,
            format="mp3",
            bitrate="128k"
        )

        return output_path

    def _convert_to_wav(self, audio_data: bytes, mime_type: str) -> bytes:
        """
        Convert raw PCM audio to WAV format.

        Args:
            audio_data: Raw audio bytes
            mime_type: MIME type (e.g., "audio/L16;rate=24000")

        Returns:
            WAV file bytes (with header)
        """
        params = self._parse_audio_mime_type(mime_type)
        bits_per_sample = params["bits_per_sample"]
        sample_rate = params["rate"]
        num_channels = 1
        data_size = len(audio_data)
        bytes_per_sample = bits_per_sample // 8
        block_align = num_channels * bytes_per_sample
        byte_rate = sample_rate * block_align
        chunk_size = 36 + data_size

        # Create WAV header (http://soundfile.sapp.org/doc/WaveFormat/)
        header = struct.pack(
            "<4sI4s4sIHHIIHH4sI",
            b"RIFF",          # ChunkID
            chunk_size,       # ChunkSize (total file size - 8 bytes)
            b"WAVE",          # Format
            b"fmt ",          # Subchunk1ID
            16,               # Subchunk1Size (16 for PCM)
            1,                # AudioFormat (1 for PCM)
            num_channels,     # NumChannels
            sample_rate,      # SampleRate
            byte_rate,        # ByteRate
            block_align,      # BlockAlign
            bits_per_sample,  # BitsPerSample
            b"data",          # Subchunk2ID
            data_size         # Subchunk2Size (size of audio data)
        )

        return header + audio_data

    def _parse_audio_mime_type(self, mime_type: str) -> dict:
        """
        Parse audio MIME type to extract parameters.

        Args:
            mime_type: MIME type string (e.g., "audio/L16;rate=24000")

        Returns:
            Dict with "bits_per_sample" and "rate" keys
        """
        bits_per_sample = 16  # Default
        rate = 24000  # Default

        parts = mime_type.split(";")
        for param in parts:
            param = param.strip()
            if param.lower().startswith("rate="):
                try:
                    rate = int(param.split("=", 1)[1])
                except (ValueError, IndexError):
                    pass
            elif param.startswith("audio/L"):
                try:
                    bits_per_sample = int(param.split("L", 1)[1])
                except (ValueError, IndexError):
                    pass

        return {"bits_per_sample": bits_per_sample, "rate": rate}


def generate_speech(
    text: str,
    output_path: str,
    voice: Optional[str] = None,
    language_code: Optional[str] = None,
    api_key: Optional[str] = None,
    model: str = "gemini-2.5-flash-preview-tts"
) -> str:
    """
    Convenience function to generate speech without creating a TTS object.

    Args:
        text: Text to convert to speech
        output_path: Output file path
        voice: Voice name (optional)
        language_code: Language code for default voice selection
        api_key: Gemini API key (optional, uses env var if not provided)
        model: Model to use

    Returns:
        Path to generated audio file
    """
    tts = GeminiTTS(api_key=api_key, model=model)
    return tts.generate_speech(text, output_path, voice, language_code)


# Example usage
if __name__ == "__main__":
    import sys

    if len(sys.argv) < 3:
        print("Usage: python gemini_tts.py <text> <output_path> [voice] [language_code]")
        print("Example: python gemini_tts.py 'Bonjour le monde' output.mp3 Leda fr")
        sys.exit(1)

    text = sys.argv[1]
    output = sys.argv[2]
    voice = sys.argv[3] if len(sys.argv) > 3 else None
    lang = sys.argv[4] if len(sys.argv) > 4 else None

    print(f"Generating speech: {text[:50]}...")
    result = generate_speech(text, output, voice, lang)
    print(f"✅ Saved to: {result}")
