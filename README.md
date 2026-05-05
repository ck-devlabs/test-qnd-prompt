STRICT AS-DISPLAYED LOC/BLDG RULE

For each visible table row, read LOC # and BLDG # only from that row’s own cell.

Do NOT carry forward LOC # or BLDG # from any previous row.
Do NOT infer that a blank cell means “same as above.”
Do NOT use visual grouping to populate missing LOC # or BLDG #.
Do NOT auto-increment, backfill, normalize, pad, or generate values.

If the LOC # cell is visually blank in that row, return:
"locationNumber": ""

If the BLDG # cell is visually blank in that row, return:
"buildingNumber": ""

Example:
If row 1 shows LOC # = 1 and BLDG # = 1,
return locationNumber = "1", buildingNumber = "1".

If row 3 shows blank LOC # and blank BLDG #,
return locationNumber = "", buildingNumber = "".

Accuracy must be based on what is visibly printed inside that row’s own LOC # and BLDG # cells only.
