#!/usr/bin/env python3
"""
Test script to verify Gemini API access with text generation.
This uses a text-only model which should be available on free tier.
"""

import os
from google import genai
from google.genai import types


def test_gemini_api():
    """Test basic Gemini API functionality."""
    api_key = os.environ.get("GEMINI_API_KEY")

    if not api_key:
        print("❌ Error: GEMINI_API_KEY environment variable not set")
        return

    print("🔑 API Key found")
    print("🚀 Testing Gemini API with text generation...\n")

    try:
        client = genai.Client(api_key=api_key)

        # Use a model that's available on free tier
        model = "gemini-2.0-flash-exp"

        prompt = "Generate a simple French A1 level sentence about coffee. Just the sentence, no explanation."

        print(f"📝 Prompt: {prompt}")
        print(f"🤖 Model: {model}\n")

        response = client.models.generate_content(
            model=model,
            contents=prompt
        )

        print("✅ Success! Generated text:")
        print("-" * 50)
        print(response.text)
        print("-" * 50)

    except Exception as e:
        print(f"❌ Error: {e}")
        if "quota" in str(e).lower():
            print("\n💡 Tip: Check your quota at https://ai.dev/usage")
        elif "billing" in str(e).lower():
            print("\n💡 Tip: Enable billing in Google Cloud Console")


if __name__ == "__main__":
    test_gemini_api()
