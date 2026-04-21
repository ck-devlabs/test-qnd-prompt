3. SAME-BLOCK ANCHORING
Treat each premises section as one independent visual block.
Within a block:
- locationNumber comes only from the numeric cell directly below "LOC #"
- buildingNumber comes only from the numeric cell directly below "BLD #"

Do not use values from another block.

4. DIRECTLY-BELOW RULE
If the numeric value for LOC # or BLD # appears directly below its label within the same block, extract it even if:
- the OCR spacing is poor
- the value is on the next line
- the surrounding fields are blank
- the address fields repeat across blocks

5. NO INVENTION
- Do not carry LOC # from a prior block
- Do not carry BLD # from a prior block
- Do not assume LOC = 1 because nearby blocks show 1
- Do not assume building sequence
- Do not pair a visible BLD # with a guessed LOC #
- Do not create missing values from repetition patterns
