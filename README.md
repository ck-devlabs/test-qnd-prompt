STRICT EXTRACTION MODE:

- Each row MUST be treated independently.
- NEVER use previous or next row values to fill missing fields.
- NEVER assume continuity of LOC # or HAZ #.
- If a cell is blank, empty, or visually missing → return null.
- Do NOT propagate values vertically.

- ROW DEFINITION RULE:

- A row is defined ONLY by values that appear in the same horizontal line.
- If LOC # cell is empty in that row → locationNumber = null.
- Do NOT associate classification rows with previous LOC # unless LOC # is explicitly present in the same row.

- SPARSE ROW HANDLING:

- Rows may contain only partial data (e.g., classification without LOC #).
- These rows MUST still be returned as separate entries.
- Missing LOC # or HAZ # MUST remain null.
- Do NOT merge sparse rows with previous rows.

- COLUMN ALIGNMENT RULE:

- Extract values strictly based on column position.
- Ignore visual grouping or logical grouping.
- Do NOT realign or restructure rows.

- 
