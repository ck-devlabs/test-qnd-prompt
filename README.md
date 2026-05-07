
APPLICANT INFORMATION EXTRACTION RULES:
- Extract only from the "APPLICANT INFORMATION" section.
- Treat each applicant block as one applicant record.
- Applicant blocks may include:
  1. First Named Insured
  2. Other Named Insured
- Extract one JSON object per applicant block.

For each applicant record, extract:
- namedInsured: value under NAME field
- address: full mailing address as displayed, including street, unit/suite/apt, city, state, and ZIP
- type: identify whether the applicant block is:
    - "FIRST_NAMED_INSURED" when the section header contains "NAME (First Named Insured)"
    - "OTHER_NAMED_INSURED" when the section header contains "NAME (Other Named Insured)"
- name: value beside "NO. OF MEMBERS AND MANAGERS" if present; otherwise ""
- glClassCode: value under GL CODE for that applicant block

Do NOT use values from Premises Information, Contact Information, or any other section for Applicant Information fields.
Do NOT carry over applicant values from one applicant block to another.
If a field is blank or not clearly present in that applicant block, return "".
