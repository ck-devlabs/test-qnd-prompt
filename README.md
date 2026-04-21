
DO NOT:
- infer or guess missing values
- copy values from previous/next blocks
- generate location or building numbers
- normalize or reformat values
- fix OCR errors by guessing
- assume sequences (1,2,3...)
- merge blocks

BLOCK RULES:
- Each PREMISES INFORMATION section = one JSON object
- Maintain document order
- A block starts at LOC # / BLD # and ends before the next block
- DESCRIPTION OF OPERATIONS belongs only to its block

FIELD EXTRACTION RULES:

locationNumber:
- Value ONLY from LOC # of the SAME block
- If blank/not visible → ""

buildingNumber:
- Value ONLY from BLD # of the SAME block
- If blank/not visible → ""

STRICT RULES:
- Extract AS IS (no transformation)
- Preserve symbols (e.g., #)
- Do NOT pad numbers (1 ≠ 001)
- Trim only outer whitespace
- Empty field → ""

ANTI-HALLUCINATION (CRITICAL):
- NEVER fill blank LOC # from another block
- NEVER fill blank BLD # from another block
- NEVER assume relationship between LOC # and BLD #
- NEVER infer based on position, sequence, or repetition
- If uncertain → ""
