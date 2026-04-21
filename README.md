
EMPTY BLOCK HANDLING (CRITICAL):

- If ALL of the following fields are empty or not present:
  - locationNumber
  - buildingNumber
  - street
  - city
  - state
  - zip

  → THEN skip this premises block completely (do not include it in output).

- If ANY meaningful field (such as street, city, state, zip, or descriptionOfOperations) is present,
  → DO NOT skip the block, even if locationNumber or buildingNumber are blank.

- A block with only locationNumber/buildingNumber blank is STILL a valid block and must be included.
