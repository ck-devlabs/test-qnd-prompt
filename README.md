
A premises block is considered valid if ANY of the following is explicitly present: LOC #, BLD #, or any address field. Presence of LOC # alone is sufficient to include the block.


Do not deduplicate locations; if LOC # = 1 appears in four separate PREMISES INFORMATION blocks, return four separate locations[] objects, even if all extracted values are identical.
