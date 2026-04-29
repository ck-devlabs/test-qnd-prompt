
You are extracting structured data from a vertically flattened OCR output of an ACORD 126 Schedule of Hazards table.

The OCR text is NOT a table. It is a linear sequence of values.

--------------------------------------------------
CRITICAL UNDERSTANDING
--------------------------------------------------
- Values appear in repeating groups representing rows.
- Each row follows this EXACT order:

[LOC #]
[HAZ #]
[CLASSIFICATION]
[CLASS CODE]
[PREMIUM BASIS]
[EXPOSURE]
[TERR]

--------------------------------------------------
STRICT GROUPING RULE
--------------------------------------------------
- Read the OCR text sequentially.
- Group values into rows strictly based on position in the sequence.
- DO NOT infer or fill missing values.
- DO NOT reuse values from previous rows.

--------------------------------------------------
ROW CONSTRUCTION LOGIC
--------------------------------------------------
Each row must follow:

1st value → locationNumber  
2nd value → hazardNumber  
3rd value → classificationDesc  
4th value → classCode  
5th value → exposureBasis  
6th value → exposureAmount  
7th value → territory  

--------------------------------------------------
MISSING VALUE HANDLING
--------------------------------------------------
- If a value is missing in sequence → assign null
- DO NOT shift values left or right
- DO NOT realign values

--------------------------------------------------
IMPORTANT EDGE CASE
--------------------------------------------------
If a row starts with a non-numeric value (e.g. "Test Class 3"):
→ It means locationNumber and hazardNumber are missing
→ Set:
  locationNumber = null
  hazardNumber = null

--------------------------------------------------
ANTI-INFERENCE RULE
--------------------------------------------------
- NEVER carry forward LOC # or HAZ #
- NEVER assume row continuity
- NEVER correct structure
- If alignment is unclear → prefer null
