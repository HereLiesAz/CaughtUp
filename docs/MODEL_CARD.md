# Research Agent Model Card

## Overview
A lightweight text classification model built with TensorFlow/Keras and converted to TFLite for on-device inference in the CleanUnderwear Android app.

## Capabilities
- **Primary Objective**: Distinguish between official/legal/obituary sources and social/commercial noise.
- **Legal & Court Detection**: Specifically trained to recognize patterns from PACER, state judicial branches, and clerk of court search results.
- **Obituary Intelligence**: Recognizes funeral home memorials and major obituary databases (Legacy, Dignity Memorial, NOLA.com).
- **Nickname Resilience**: Trained to maintain high accuracy across multi-cultural naming variations and common nicknames (e.g., Robert/Bob/Bobby, William/Bill/Billy).

## Input/Output
- **Input**: Raw text strings (Search result snippets).
- **Output**: Sigmoid confidence score (0.0 - 1.0). Threshold > 0.5 for official sources.

## Assets Included
- `research_agent.tflite` (Optimized Model)
- `research_agent_vocab.txt` (Preprocessing Vocabulary)
- `nicknames.json` (Demographic-specific nickname mapping)
- `scraper_triggers.json` (Regional area code and source prioritization)
