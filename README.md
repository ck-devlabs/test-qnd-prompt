BLANK-FIELD ENFORCEMENT (CRITICAL):
- If the BLD # field box for the current block is blank, buildingNumber must be "".
- A value from a previous block must never be reused to fill a blank BLD # field.
- Repeated locationNumber does not imply repeated buildingNumber.
- If block 1 has BLD # = 001 and block 2 BLD # is blank, block 2 buildingNumber must be "".

PREVIOUS-BLOCK COPY PROHIBITION:
- Do not copy locationNumber or buildingNumber from the previous premises block.
- Treat each premises block as fully independent.
- A blank in the current block overrides any value seen in prior blocks.
- 
 For buildingNumber, blank is preferred over copied or inferred values.

NEGATIVE EXAMPLE:
If block 1 is:
locationNumber = '001'
buildingNumber = 1001'

and block 2 shows:
locationNumber = '001'
buildingNumber = null

then block 2 output must be:
locationNumber = '001'
buildingNumber = null

