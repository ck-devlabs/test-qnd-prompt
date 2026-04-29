
You are given:
1. An IMAGE of the document (primary source of truth)
2. OCR TEXT (secondary helper, may be incorrect or misaligned)

==================================================
SOURCE PRIORITY (CRITICAL)
==================================================

- The IMAGE is the ONLY source of truth for:
  - row boundaries
  - column alignment
  - whether a cell is blank or populated

- OCR text MUST NOT be trusted for:
  - row grouping
  - column alignment
  - missing vs present values

- Use OCR text ONLY to read unclear characters AFTER locating the correct cell in the image.

If OCR conflicts with IMAGE → ALWAYS trust IMAGE.

==================================================
TASK
==================================================

Extract rows from the SCHEDULE OF HAZARDS table.

Columns:
LOC # | HAZ # | CLASSIFICATION | CLASS CODE | PREMIUM BASIS | EXPOSURE 

==================================================
VISUAL EXTRACTION RULES (MANDATORY)
==================================================

For EACH ROW in the table:

1. Identify the horizontal row visually in the IMAGE.
2. For that row, read each column cell independently.

For each column:

- If text is visibly present inside the cell → extract it.
- If the cell is visually empty → return null.
- Do NOT infer from other rows.
- Do NOT use OCR to fill empty cells.

==================================================
CRITICAL ANTI-PROPAGATION RULE
==================================================

- NEVER carry LOC # from previous row.
- NEVER carry HAZ # from previous row.
- NEVER assume blank means "same as above".
- If LOC # cell is empty → locationNumber = null.
- If HAZ # cell is empty → hazardNumber = null.

==================================================
SPARSE ROW HANDLING
==================================================

- Rows may have empty LOC # and HAZ # but valid classification.
- These MUST be returned as separate rows.
- Do NOT merge with previous row.


==================================================
FIELD MAPPING
==================================================

- locationNumber → LOC # column
- hazardNumber → HAZ # column
- classificationDesc → CLASSIFICATION column
- classCode → CLASS CODE column
- exposureBasis → PREMIUM BASIS column
- exposureAmount → EXPOSURE column

==================================================
OCR USAGE RULE
==================================================

Use OCR ONLY for:
- reading characters inside a visually identified cell

DO NOT use OCR to:
- determine row boundaries
- determine missing values
- reconstruct table structure

==================================================
NEGATIVE RULES (STRICT)
==================================================

DO NOT:
- auto-increment values
- backfill missing LOC #
- backfill missing HAZ #
- merge rows
- shift values across columns
- guess missing data
==================================================
FINAL RULE
==================================================

If uncertain, return null.

Accuracy based on IMAGE > completeness.
