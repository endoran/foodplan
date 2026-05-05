# Scan Training Data — Pattern Lessons

Collected from production scans (gomez instance, May 2026). These patterns document systematic errors made by qwen3.5:9b during vision-based recipe extraction.

## Pattern 1: Parenthetical Weight → OZ

**When the recipe says:** `X (Y-ounce) can/package/container/jar <item>`
**Model error:** Extracts as quantity=X, unit=WHOLE (or confuses "15" with "1.5")
**Correct extraction:** quantity=Y, unit=OZ

Examples:
- "1 (15-ounce) can pinto beans" → `{"quantity": 15, "unit": "OZ"}`
- "1 (8-ounce) package cream cheese" → `{"quantity": 8, "unit": "OZ"}`
- "1 (14-ounce) container cottage cheese" → `{"quantity": 14, "unit": "OZ"}`
- "2 (10-ounce) packages spinach" → `{"quantity": 20, "unit": "OZ"}`
- "2 (12-ounce) jars spaghetti sauce" → `{"quantity": 2, "unit": "WHOLE", "prepNote": "12-ounce jars"}` (when jars are the natural unit)

**Root cause:** Model confuses the count (X) with the measurement (Y), or misreads "15" as decimal "1.5".

## Pattern 2: Small Fractions Misread

**When the recipe says:** ⅛, ¼, ⅓ (Unicode or written as "1/8", "1/4", "1/3")
**Model error:** Rounds up to 0.5 or larger
**Correct extraction:** 0.125, 0.25, 0.333

Examples:
- "⅛ teaspoon cayenne pepper" → `{"quantity": 0.125, "unit": "TSP"}` (NOT 0.5)
- "¼ cup Parmesan" → `{"quantity": 0.25, "unit": "CUP"}` (NOT 0.5)

**Root cause:** Model may not have strong training on Unicode fraction glyphs ⅛ (U+215B) and defaults to ½ when uncertain.

## Pattern 3: Digit vs Decimal Confusion

**When the recipe says:** Numbers in parenthetical context
**Model error:** "15" → "1.5", "14" → "1.4", "12" → "1.2"
**Correct:** These are integers (ounces), not decimals

**Root cause:** The model sees "(15-ounce)" and interprets the "1" and "5" as "1.5" — likely because it associates recipe quantities with small decimal numbers (0.5-4.0 range) and force-fits the larger number into that expected range.

## Prompt Fixes Applied

Added to both VISION_PROMPT and RULES:
1. Explicit ⅛ = 0.125 in fraction table
2. PARENTHETICAL WEIGHT pattern with examples
3. "NEVER confuse 15 with 1.5" warning
4. Reinforced small fraction reading in vision-specific prompt section

## Training Pair Files

- `001-lazy-lasagna.json` — 6 corrections (fractions, parenthetical weight)
- `002-ranch-hand-taco-salad.json` — 6 corrections (canned goods 15→1.5 confusion)
