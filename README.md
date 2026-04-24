
PREMISES INFORMATION EXTRACTION RULES:

1. Treat each repeated "PREMISES INFORMATION" block as a separate premises record.

2. A premises record must be created when the block contains ANY real premises data, including:
   - LOC #
   - BLD #
   - street
   - city
   - state
   - zip

3. Do NOT require both LOC # and BLD # to be present.

4. Extract LOC # only from the value physically shown beside the "LOC #" label in that same block.

5. Extract BLD # only from the value physically shown beside the "BLD #" label in that same block.

6. Do NOT infer, carry forward, auto-increment, copy, or generate LOC # or BLD #.

7. If LOC # is blank in the PDF, return "".
   If BLD # is blank in the PDF, return "".

8. Do not skip a block only because LOC # or BLD # is blank.
   Skip a block only when the entire premises block has no real business data.

9. For ACORD 125 repeated premises sections, preserve every non-empty repeated block even if street/city/state/zip values look duplicated.

10. Return records in visual top-to-bottom order from the PDF.

IMPORTANT:
Repeated premises blocks are not duplicates. Do not collapse them into one record even if address fields are the same. Each visible LOC#/BLD# block is a separate premises record.
