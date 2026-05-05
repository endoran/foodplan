# Scan Training Data — Pattern Lessons

Collected from production scans (gomez instance). These document systematic errors made by qwen3.5:9b (VISION) and qwen3:14b (TEXT_LLM) during recipe extraction.

## Error Pattern Summary (across all 3 corrected pairs)

| Pattern | Count | Description |
|---------|-------|-------------|
| PARENTHETICAL_WEIGHT | 12 | "X (Y-ounce) can/pkg" → should be qty=Y, unit=OZ |
| SMALL_FRACTION | 6 | ⅛, ¼, ¾ misread as ½ or larger |
| DIGIT_DECIMAL_CONFUSION | 2 | "15" in "(15-ounce)" → "1.5" |
| SERVINGS_MISREAD | 2 | "FEEDS 6 TO 8" → 4, handwritten servings wrong |
| HANDWRITTEN_QUANTITY | 2 | Handwritten numbers misread (vision tier) |
| FRACTION_UNIT_COMBO | 1 | "1½ tablespoons" → "2 TSP" (wrong qty AND unit) |
| TITLE_VERBATIM | 1 | Missed "Lazy" from recipe title |
| PREP_IN_NAME | 1 | "thawed" in ingredient name instead of prep note |
| OCR_GIBBERISH | 1 | "⅓" rendered as "¥:" |

## Key Lessons for Prompt Engineering

### 1. Parenthetical Weight (highest impact — 12 errors)
When recipe text says `X (Y-ounce) can/package/container/jar <item>`:
- Extract as `quantity = X × Y`, `unit = OZ`
- Single container: "1 (15-ounce) can beans" → qty=15, unit=OZ
- Multiple: "2 (12-ounce) jars sauce" → qty=24, unit=OZ
- Model defaults to PIECE/WHOLE and uses the count as qty

### 2. Small Fractions (second highest — 6 errors)
- ⅛ = 0.125 (model reads as 0.5)
- ¼ = 0.25 (model reads as 0.5 or 0.75)
- ¾ = 0.75 (model reads as 0.5)
- "Rounded ½ cup" = 0.5 (model read as 2)

### 3. Digit/Decimal Confusion (2 errors)
- "15" in "(15-ounce)" → model produces "1.5"
- Seems to force-fit larger integers into small-decimal recipe range

## Production Data (MongoDB)

- Database: `food` on `foodplan-gomez-mongo-0` (home-prod namespace)
- Collection: `training_pairs`
- Total pairs: 4 (3 with corrections, 1 without)
- Fields: `modelOutput`, `correctedOutput`, `extractionTier`, `imageData` (binary), `hasCorrections`

## Training Pair Files

| File | Recipe | Tier | Corrections |
|------|--------|------|-------------|
| `001-lazy-lasagna.json` | Lazy Spinach Lasagna | TEXT_LLM | 13 |
| `002-ranch-hand-taco-salad.json` | Ranch Hand Taco Salad | TEXT_LLM | 12 |
| `003-yorkshire-pudding.json` | Yorkshire Pudding | VISION | 4 |

## Prompt Fixes Applied (this PR)

Added to `OllamaRecipeExtractor.java` RULES + VISION_PROMPT:
1. Explicit ⅛ = 0.125 in fraction table (was missing)
2. PARENTHETICAL WEIGHT pattern with examples
3. "NEVER confuse 15 with 1.5" warning
4. Reinforced in vision-specific section for immediate model context
