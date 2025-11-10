# Gemini TTS Scripts

Text-to-Speech generation scripts using Google's Gemini API.

## Overview

This directory contains tools for generating speech audio from text using Gemini's TTS models:

- **`gemini_tts.py`** - Modular TTS library with clean API
- **`batch_generate_audio.py`** - Batch processor for multiple text files
- **`gemini_text_to_speech.py`** - Original single-file script (deprecated)
- **`test_gemini_text.py`** - API testing script

## Setup

### 1. Install Dependencies

```bash
pip install google-genai
```

### 2. Get API Key

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Create or select a project
3. Generate an API key
4. Set environment variable:

```bash
export GEMINI_API_KEY="your-api-key-here"
```

Or add to your `.bashrc` / `.zshrc`:

```bash
echo 'export GEMINI_API_KEY="your-api-key-here"' >> ~/.zshrc
source ~/.zshrc
```

## Usage

### Quick Start - Single Text

Generate audio for a single piece of text:

```bash
# Using the library directly
python3 gemini_tts.py "Bonjour le monde" output.wav Leda fr

# Parameters:
# 1. Text to convert
# 2. Output file path
# 3. Voice name (optional, defaults based on language)
# 4. Language code (optional, e.g., fr, de, en)
```

### Batch Processing

Generate audio for all text files in `texts/generated`:

```bash
# Dry run (see what will be processed)
python3 batch_generate_audio.py --dry-run

# Process all files
python3 batch_generate_audio.py

# The script will:
# - Find all JSON files in texts/generated
# - Generate audio with proper rate limiting (20s between requests)
# - Save progress (can resume if interrupted)
# - Output audio to audio_generated/ directory
```

### Batch Processing Options

```bash
# Custom directories
python3 batch_generate_audio.py \
  --input texts/generated \
  --output audio_output

# Use different model
python3 batch_generate_audio.py --model gemini-2.5-pro-preview-tts

# Adjust rate limiting (e.g., 30 seconds between requests)
python3 batch_generate_audio.py --rate-limit 30

# Start fresh (ignore previous progress)
python3 batch_generate_audio.py --no-resume

# Resume after interruption (default behavior)
python3 batch_generate_audio.py --resume
```

## Rate Limits

**Important:** Gemini TTS has a rate limit of **3 requests per minute (RPM)**.

The batch processor automatically handles this by waiting 20 seconds between requests. For 17 files, expect:
- Processing time: ~6 minutes
- One audio file generated every 20 seconds

## Available Models

- `gemini-2.5-flash-preview-tts` (default) - Faster, lower cost
- `gemini-2.5-pro-preview-tts` - Higher quality

## Available Voices

### Default Voice Mapping

The library automatically selects voices based on language:

- **French (fr)**: Leda (female)
- **German (de)**: Kore (female)
- **English (en)**: Puck (neutral)

### All Available Voices

According to [Google's documentation](https://ai.google.dev/gemini-api/docs/speech-generation):

- Aoede, Charon, Fenrir, Kore, Leda, Orbit, Puck, Zephyr (and more)
- 30 prebuilt voices total
- 24 languages supported

## File Formats

### Input Format

Text files should be JSON with this structure:

```json
{
  "title": "À la Boulangerie",
  "language_code": "fr",
  "level": "A1",
  "topic": "shopping",
  "content": "Your text content here..."
}
```

Required fields:
- `content` - The text to convert to speech
- `language_code` - ISO language code (fr, de, en, etc.)

### Output Format

- Format: WAV (uncompressed audio)
- Encoding: PCM, 16-bit, mono
- Sample Rate: 24,000 Hz
- Typical size: ~2-3 MB per minute of audio

## Progress Tracking

The batch processor saves progress to `audio_generation_progress.json`:

```json
{
  "completed": [
    "fr/fr_a1_bakery.json",
    "de/de_a1_cafe.json"
  ],
  "failed": []
}
```

This allows you to:
- Resume interrupted runs
- Skip already processed files
- Track failures

Delete this file to start fresh, or use `--no-resume`.

## Examples

### Generate Audio for All Texts

```bash
export GEMINI_API_KEY="your-key"
python3 batch_generate_audio.py
```

Output:
```
============================================================
🎵 Batch Audio Generation Starting
============================================================
Found 17 text files in texts/generated
...
🎙️  Processing (1/17): fr/fr_a1_bakery.json
   Language: fr
   Text length: 570 chars
   ✅ Saved: audio_generated/fr/fr_a1_bakery.wav (2.84 MB)
⏳ Rate limit: waiting 20.0s...
...
============================================================
📊 Processing Summary
============================================================
Total files: 17
Processed: 17
Skipped: 0
Failed: 0
Duration: 5.7 minutes
============================================================
```

### Generate Single File with Custom Voice

```python
from gemini_tts import generate_speech

generate_speech(
    text="Guten Tag! Wie geht es Ihnen?",
    output_path="greeting.wav",
    voice="Kore",
    language_code="de"
)
```

### Use the Library in Your Code

```python
from gemini_tts import GeminiTTS

# Initialize once
tts = GeminiTTS(api_key="your-key")

# Generate multiple files
texts = [
    ("Bonjour", "hello_fr.wav", "fr"),
    ("Guten Tag", "hello_de.wav", "de"),
]

for text, output, lang in texts:
    tts.generate_speech(text, output, language_code=lang)
```

## Troubleshooting

### Error: "Quota exceeded"

You've hit the rate limit. Options:
1. Wait 1 minute and try again
2. Increase `--rate-limit` (e.g., 30 seconds)
3. Enable billing in Google Cloud Console for higher limits

### Error: "API key required"

Set the environment variable:
```bash
export GEMINI_API_KEY="your-key"
```

### Error: "Model not found"

Make sure you're using the correct model name with `-preview`:
- ✅ `gemini-2.5-flash-preview-tts`
- ❌ `gemini-2.5-flash-tts` (wrong)

### Files Taking Too Long

Processing 17 files takes ~6 minutes due to rate limiting. This is expected.

To process faster:
1. Enable billing (may allow higher rate limits)
2. Process in parallel with multiple API keys (not recommended)

## Cost

According to Google's documentation:
- TTS is currently in **Preview** status
- "Standard billing applies"
- Exact pricing not disclosed during preview
- Free tier has limited quota (appears to work for small batches)

Monitor your usage at: https://aistudio.google.com/app/apikey

## Architecture

```
scripts/genai/
├── gemini_tts.py              # Core TTS library
│   ├── GeminiTTS class        # Main TTS client
│   ├── generate_speech()      # Convenience function
│   └── WAV conversion logic   # PCM to WAV format
│
├── batch_generate_audio.py    # Batch processor
│   ├── BatchAudioGenerator    # Orchestrates batch processing
│   ├── Rate limiting          # 3 RPM enforcement
│   ├── Progress tracking      # Resume capability
│   └── CLI interface          # argparse setup
│
└── README.md                  # This file
```

## Supported Languages

According to the documentation, TTS supports 24 languages including:

- French (fr)
- German (de)
- English (en)
- Spanish (es)
- Italian (it)
- Portuguese (pt)
- And more...

## Uploading Audio to Storage

After generating audio files, upload them to MinIO (S3-compatible storage) and update the database:

### Upload Script

```bash
# Install required dependencies
pip install boto3 requests

# Dry run to see what will be uploaded
python3 upload_audio_to_storage.py --dry-run

# Upload all audio files
python3 upload_audio_to_storage.py
```

### How It Works

The upload script:
1. Scans `audio_generated/` for `.wav` files
2. Matches each audio file to its corresponding text in the database
3. Uploads the audio file to MinIO (bucket: `vocabee-audio`)
4. Updates the reading text with the audio URL via backend API

### Upload Options

```bash
# Custom backend URL
python3 upload_audio_to_storage.py --backend-url http://localhost:8080

# Custom S3 endpoint
python3 upload_audio_to_storage.py --s3-endpoint http://localhost:9000

# Custom audio directory
python3 upload_audio_to_storage.py --audio-dir /path/to/audio/files
```

### Prerequisites

Before uploading:
1. Start MinIO: `docker-compose -f docker-compose.dev.yml up -d`
2. Start backend: `./gradlew bootRun`
3. Ensure audio files are generated: `python3 batch_generate_audio.py`

## Next Steps

1. **Test with sample files**:
   ```bash
   python3 batch_generate_audio.py --dry-run
   ```

2. **Generate audio for a few files**:
   ```bash
   # Interrupt with Ctrl+C after 2-3 files to test resume
   python3 batch_generate_audio.py
   ```

3. **Resume processing**:
   ```bash
   python3 batch_generate_audio.py --resume
   ```

4. **Upload audio to storage**:
   ```bash
   python3 upload_audio_to_storage.py --dry-run
   python3 upload_audio_to_storage.py
   ```

5. **Integrate audio into your app**:
   - Add audio playback to reading interface
   - Display audio indicators on text cards
   - Test playback functionality

## References

- [Gemini API Speech Generation Docs](https://ai.google.dev/gemini-api/docs/speech-generation)
- [Google AI Studio](https://aistudio.google.com/)
- [Python SDK Documentation](https://github.com/googleapis/python-genai)
