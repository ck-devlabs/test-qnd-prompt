
locationNumber:
- Extract from the LOC # field of the current premises block only.
- If the current block shows 1, return "001".
- If LOC # is blank or not clearly visible in that block, return "".

buildingNumber:
- Extract from the BLD # field of the current premises block only.
- If the current block shows 1, return "001", 2 -> "002", 3 -> "003", etc.
- If BLD # is blank or not clearly visible in that block, return "".

- CRITICAL ASSOCIATION RULE:
Do not assign a number to locationNumber or buildingNumber unless that number is visibly tied to the corresponding LOC # or BLD # field in the same block.
A number visible elsewhere in the block must not be used.

For LOC # and BLD #, prefer exact field matching over conservative blanking.
Only return blank when the value is truly not visible in that field.
