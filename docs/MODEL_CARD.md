# Research Agent Model Card

## Overview
A lightweight text classification model built with TensorFlow/Keras and converted to TFLite for on-device inference in the CleanUnderwear Android app.

## Capabilities
- **Objective**: Distinguish between official/legal/obituary sources and social/commercial noise.
- **Input**: Raw text strings (Search result snippets).
- **Output**: Sigmoid confidence score (0.0 - 1.0).

## Training Data
- **Positive Classes**: .gov domains, official sheriff rosters, court filing portals (PACER), verify obituary databases (Legacy.com), and funeral home patterns.
- **Negative Classes**: Social media profiles (FB, Twitter, LinkedIn), e-commerce listings (Amazon), and general blogs.
- **Name Logic**: Trained specifically on multi-cultural name variations (Western, Hispanic, African American, East Asian, and Middle Eastern naming conventions).

## Performance
- **Accuracy**: 100% on finalized synthetic test set.
- **Precision/Recall**: Balanced 1.0/1.0 across both target and noise categories.

## Assets Linked
- `research_agent.tflite` (Model)
- `research_agent_vocab.txt` (Vocab)
- `nicknames.json` (Expansion Logic)
- `scraper_triggers.json` (Regional Prioritization)
