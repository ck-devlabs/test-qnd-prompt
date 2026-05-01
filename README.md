FINAL LOCATIONS FILTER: Before returning JSON, remove any locations[] object where locationNumber, buildingNumber, street, city, state, and postalCode are all null, empty string, whitespace, or not explicitly visible in the PDF.

Use null for missing values; never create a locations[] object only to hold null/empty fields.
