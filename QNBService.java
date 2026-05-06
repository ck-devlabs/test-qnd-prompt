import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode; // Added missing import
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class QNBService {

    private static final int DAYS_WINDOW = 182;

    private static final DateTimeFormatter[] PARTIAL_DATE_FORMATTERS = {
       
    // "9 Oct", "17 Apr", "1 Jan"
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM")
        .toFormatter(Locale.ENGLISH),

    // "9 October", "17 April", "1 January"
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMMM")
        .toFormatter(Locale.ENGLISH),

    // "Oct 9", "Apr 17", "Jan 1"
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMM d")
        .toFormatter(Locale.ENGLISH),

    // "Oct 09", "Apr 17"  (zero-padded)
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMM dd")
        .toFormatter(Locale.ENGLISH),

    // "October 9", "April 17", "May 1"  ← covers "May 1st" after normalization
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMMM d")
        .toFormatter(Locale.ENGLISH),

    // "October 09", "April 17"  (zero-padded)
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMMM dd")
        .toFormatter(Locale.ENGLISH),

    // "9 Oct 2025", "17 Apr 2025"  (year ignored, MonthDay extracted)
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM yyyy")
        .toFormatter(Locale.ENGLISH),

    // "9 October 2025", "17 April 2025"
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMMM yyyy")
        .toFormatter(Locale.ENGLISH),

    // "October 9 2025", "April 17 2025"
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMMM d yyyy")
        .toFormatter(Locale.ENGLISH),

    // "Oct 9 2025", "Apr 17 2025"
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMM d yyyy")
        .toFormatter(Locale.ENGLISH),
};


    public LocalDate resolveQuoteNeedByDate(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.has("quoteNeedByDate")) {
            throw new IllegalArgumentException("JsonNode is missing 'quoteNeedByDate' field");
        }

        String rawDate = jsonNode.get("quoteNeedByDate").asText().trim();
        if (rawDate.isEmpty()) {
            throw new IllegalArgumentException("'quoteNeedByDate' field is empty");
        }

        MonthDay monthDay = parsePartialDate(rawDate);
        
        // Fix: Pass LocalDate.now() to match the method signature
        return resolveWithinWindow(monthDay, LocalDate.now());
    }

private static MonthDay parsePartialDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            //log.error("Null or blank date input");
            return null;
        }
        String normalized = normalize(rawDate);
        try {
            if (normalized.matches("\\d{1,2}")) {
                int day = Integer.parseInt(normalized);
                if (day < 1 || day > 31) {
                    //log.error("Day out of valid range: {}", day);
                    return null;
                }
                return resolveMonthDay(LocalDate.now().getMonth(), day);
            }
            if (normalized.matches("\\d+")) {
               // log.error("Day number out of range: {}", normalized);
                return null;
            }
            // Strip trailing year if present e.g. "15 May 2024"
            normalized = normalized.replaceAll("\\b\\d{4}\\b", "").replaceAll("\\s+", " ").trim();

            TemporalAccessor accessor = PARTIAL_DATE_FORMATTERS.parse(normalized);
            int day     = accessor.get(ChronoField.DAY_OF_MONTH);
            Month month = Month.of(accessor.get(ChronoField.MONTH_OF_YEAR));
            if (day < 1) {
                //log.error("Invalid day parsed: {}", day);
                return null;
            }
            return resolveMonthDay(month, day);
        } catch (Exception ignored) {
            //log.error("Error parsing partial date {}", normalized, ignored);
            return null;
        }
    }

    /**
     * Returns a valid MonthDay, rolling over to next month's 1st if the day
     * exceeds the month's maximum (e.g. April 31 → May 1).
     */
    private static MonthDay resolveMonthDay(Month month, int day) {
        int maxDay = month.maxLength(); // maxLength accounts for leap year (Feb → 29)

        if (day <= maxDay) {
            return MonthDay.of(month, day);
        }

        // Day exceeds month's max → roll over to 1st of next month
        Month nextMonth = month.plus(1); // wraps Dec → Jan automatically
        //log.warn("Date {}/{} is invalid, rolling over to {}/1", day, month, nextMonth);
        return MonthDay.of(nextMonth, 1);
    }

    static String normalize(String rawDate) {
        String n = rawDate.replaceAll("(?i)\\bthe\\b", "").trim();       // strip "the"
        n = n.replaceAll("(?i)(?<=\\d)(st|nd|rd|th)\\b", "").trim();    // strip ordinals
        n = n.replaceAll("(?i)\\bof\\b", "").trim();                     // strip "of"
        n = n.replaceAll("\\s+", " ").trim();                            // collapse spaces
        return n;
    }

    /**
     * Finds the occurrence of the MonthDay that falls within the 
     * next 182 days. If multiple or none, it picks the one closest 
     * to the window start (the "soonest" valid date).
     */
    private LocalDate resolveWithinWindow(MonthDay monthDay, LocalDate baseDate) {
    LocalDate best = null;
    long smallestDiff = Long.MAX_VALUE;

    for (int yearOffset = -1; yearOffset <= 1; yearOffset++) {
        int targetYear = baseDate.getYear() + yearOffset;

        // Skip Feb 29 on non-leap years explicitly
        if (monthDay.equals(MonthDay.of(2, 29)) && !java.time.Year.isLeap(targetYear)) {
            continue;
        }

        LocalDate candidate = monthDay.atYear(targetYear);
        long daysFromToday = ChronoUnit.DAYS.between(baseDate, candidate);

        if (daysFromToday >= 0 && daysFromToday <= DAYS_WINDOW) {
            if (daysFromToday < smallestDiff) {
                smallestDiff = daysFromToday;
                best = candidate;
            }
        }
    }

    if (best == null) {
        // Fallback: find the next future occurrence after the window
        // Fix: compare the full date, not just the month number
        LocalDate sameYear = monthDay.equals(MonthDay.of(2, 29))
            ? null  // handle leap separately
            : monthDay.atYear(baseDate.getYear());

        if (sameYear != null && sameYear.isAfter(baseDate)) {
            // same year date is in the future but beyond window → use it
            return sameYear;
        } else {
            // same year date is in the past or today → use next year
            int nextYear = baseDate.getYear() + 1;
            // For Feb 29, find the next leap year
            if (monthDay.equals(MonthDay.of(2, 29))) {
                while (!java.time.Year.isLeap(nextYear)) nextYear++;
            }
            return monthDay.atYear(nextYear);
        }
    }

    return best;
}

    public JsonNode resolveAndOverrideQuoteNeedByDate(JsonNode jsonNode) {
        LocalDate resolvedDate = resolveQuoteNeedByDate(jsonNode);

        if (!(jsonNode instanceof ObjectNode)) {
            throw new IllegalArgumentException("JsonNode must be an ObjectNode to allow field override");
        }

        ObjectNode objectNode = (ObjectNode) jsonNode;
        objectNode.put("quoteNeedByDate", resolvedDate.toString()); 

        return objectNode;
    }


    public String buildRowsJson(DocumentTable table) throws Exception {

    Map<Integer, Map<Integer, String>> rows = new TreeMap<>();

    // Step 1: Build row → column map
    for (DocumentCell cell : table.getCells()) {

        if (cell.getRowIndex() == null || cell.getColumnIndex() == null) {
            continue;
        }

        int row = cell.getRowIndex();
        int col = cell.getColumnIndex();

        String value = cell.getContent() == null ? "" : cell.getContent().trim();

        rows.computeIfAbsent(row, r -> new TreeMap<>())
                .merge(col, value, (a, b) -> a + "\n" + b);
    }

    // Step 2: Detect columns dynamically
    int headerRow = -1;
    Integer locCol = null;
    Integer bldgCol = null;
    Integer descCol = null;

    for (Map.Entry<Integer, Map<Integer, String>> entry : rows.entrySet()) {

        int rowIndex = entry.getKey();

        for (Map.Entry<Integer, String> colEntry : entry.getValue().entrySet()) {

            String text = normalize(colEntry.getValue());

            if (text.contains("LOC")) {
                locCol = colEntry.getKey();
                headerRow = rowIndex;
            }

            if (text.contains("BLDG")) {
                bldgCol = colEntry.getKey();
                headerRow = rowIndex;
            }

            if (text.contains("DESCRIPTION")) {
                descCol = colEntry.getKey();
                headerRow = rowIndex;
            }
        }

        if (locCol != null && bldgCol != null && descCol != null) {
            break;
        }
    }

    if (locCol == null || bldgCol == null || descCol == null) {
        throw new IllegalStateException("Could not detect required columns");
    }

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode locationsArray = mapper.createArrayNode();

    String currentLoc = null;
    String currentBldg = null;
    StringBuilder addressBuffer = new StringBuilder();

    for (Map.Entry<Integer, Map<Integer, String>> entry : rows.entrySet()) {

        int rowIndex = entry.getKey();

        if (rowIndex <= headerRow) {
            continue;
        }

        Map<Integer, String> cols = entry.getValue();

        String loc = cols.getOrDefault(locCol, "").trim();
        String bldg = cols.getOrDefault(bldgCol, "").trim();
        String desc = normalize(cols.getOrDefault(descCol, ""));

        // Skip label rows
        if (desc.equalsIgnoreCase("ADDRESS OF PROPERTY")) {
            continue;
        }

        boolean isStartRow = !loc.isBlank() && !bldg.isBlank();

        if (isStartRow) {

            if (currentLoc != null) {
                ObjectNode obj = mapper.createObjectNode();
                obj.put("locationNumber", currentLoc);
                obj.put("buildingNumber", currentBldg);
                obj.put("address", cleanAddress(addressBuffer.toString()));
                locationsArray.add(obj);
            }

            currentLoc = loc;
            currentBldg = bldg;
            addressBuffer = new StringBuilder();
        }

        if (currentLoc != null && !desc.isBlank()) {
            addressBuffer.append(desc).append(" ");
        }
    }

    // flush last record
    if (currentLoc != null) {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("locationNumber", currentLoc);
        obj.put("buildingNumber", currentBldg);
        obj.put("address", cleanAddress(addressBuffer.toString()));
        locationsArray.add(obj);
    }

    root.set("locations", locationsArray);

    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
}

    public class LocationDto {
    private String locationNumber;
    private String buildingNumber;
    private String address;

    public LocationDto(String locationNumber, String buildingNumber, String address) {
        this.locationNumber = locationNumber;
        this.buildingNumber = buildingNumber;
        this.address = address;
    }

    // getters/setters
}

    private String normalize(String value) {
    if (value == null) return "";
    return value
            .toUpperCase()
            .replace("\n", " ")
            .replaceAll("\\s+", " ")
            .trim();
}

private String cleanAddress(String value) {
    if (value == null) return "";
    return value
            .replace("\n", " ")
            .replaceAll("\\s+", " ")
            .trim();
}








     /**
     * Main entry point.
     * Combines all tables + raw text (non-table content) into a single
     * GPT-ready prompt string.
     *
     * @param result  AnalyzeResult from Azure Document Intelligence poller.getFinalResult()
     * @return        A formatted string ready to be sent to GPT
     */
    public static String buildGptInput(AnalyzeResult result) {
        StringBuilder sb = new StringBuilder();
 
        // --- Section 1: Raw text for header/non-table fields ---
        sb.append("=== FORM HEADER / NON-TABLE FIELDS ===\n");
        if (result.getContent() != null && !result.getContent().isBlank()) {
            sb.append(result.getContent().trim());
        } else {
            sb.append("(none)");
        }
        sb.append("\n\n");
 
        // --- Section 2: Tables in Markdown format ---
        List<DocumentTable> tables = result.getTables();
        if (tables == null || tables.isEmpty()) {
            sb.append("=== TABLES ===\n(no tables found)\n");
        } else {
            sb.append("=== TABLES ===\n");
            for (int i = 0; i < tables.size(); i++) {
                sb.append("TABLE ").append(i + 1).append(":\n");
                sb.append(tableToMarkdown(tables.get(i)));
                sb.append("\n");
            }
        }
 
        return sb.toString();
    }
 
    /**
     * Converts a single DocumentTable into a Markdown table string.
     * Empty cells are preserved as blank (between pipes), which is
     * critical so GPT doesn't skip or hallucinate missing fields.
     *
     * @param table  DocumentTable from AnalyzeResult
     * @return       Markdown-formatted table string
     */
    public static String tableToMarkdown(DocumentTable table) {
        int rowCount = table.getRowCount();
        int colCount = table.getColumnCount();
 
        // Pre-fill grid with empty strings — this ensures blank cells are explicit
        String[][] grid = new String[rowCount][colCount];
        for (String[] row : grid) {
            Arrays.fill(row, "");
        }
 
        // Fill in values from DocumentTableCell
        for (DocumentTableCell cell : table.getCells()) {
            int row = cell.getRowIndex();
            int col = cell.getColumnIndex();
 
            // Bounds check (defensive)
            if (row < rowCount && col < colCount) {
                String content = cell.getContent();
                grid[row][col] = (content != null) ? content.trim() : "";
            }
        }
 
        return gridToMarkdown(grid);
    }
 
    /**
     * Converts a 2D String grid to a Markdown table.
     * Row 0 is treated as the header row.
     *
     * @param grid  2D array of cell values
     * @return      Markdown table string
     */
    private static String gridToMarkdown(String[][] grid) {
        if (grid.length == 0) return "(empty table)\n";
 
        StringBuilder sb = new StringBuilder();
 
        for (int r = 0; r < grid.length; r++) {
            // Build row
            sb.append("| ");
            sb.append(String.join(" | ", grid[r]));
            sb.append(" |\n");
 
            // Add separator line after header row (row 0)
            if (r == 0) {
                sb.append("| ");
                for (int c = 0; c < grid[r].length; c++) {
                    sb.append("--- |");
                    if (c < grid[r].length - 1) sb.append(" ");
                }
                sb.append("\n");
            }
        }
 
        return sb.toString();
    }
 
    /**
     * Builds the full GPT system prompt.
     * Pair this with buildGptInput() as the user message.
     *
     * @return System prompt string for GPT
     */
    public static String buildSystemPrompt() {
        return """
                You are an insurance form data extraction assistant.
                You will receive content extracted from insurance forms via Azure Document Intelligence.
                The content has two sections:
                  1. FORM HEADER / NON-TABLE FIELDS - raw text from headers and non-table regions
                  2. TABLES - one or more Markdown-formatted tables (empty cells are blank between pipes)
                
                Your task:
                - Extract ALL fields from the header section into a flat JSON object called "headerFields"
                - Extract ALL tables into a JSON array called "tables"
                  - Each table has a "tableIndex" (1-based) and a "rows" array
                  - Each row is a JSON object where keys are the column headers (camelCase)
                  - IMPORTANT: Preserve empty string "" for ALL blank cells — do NOT skip them
                  - If no clear header row exists, use keys: col_0, col_1, col_2, etc.
                
                ADDRESS PARSING RULES (apply to every table row):
                - Insurance forms often spread an address across multiple lines within the same cell,
                  or across adjacent rows. Reconstruct the full address and split it into:
                    "street"  - street number and name (e.g. "15565 County Rd #517")
                    "city"    - city name (e.g. "Dexter")
                    "state"   - 2-letter state code (e.g. "MO")
                    "zip"     - ZIP or ZIP+4 code (e.g. "63841")
                - If any address component cannot be determined, use "".
                - "locationNumber" maps to the LOC # column (or equivalent). Use "" if blank.
                - "buildingNumber" maps to the BLDG # column (or equivalent). Use "" if blank.
                - These five fields MUST appear in every row regardless of form layout:
                    locationNumber, buildingNumber, street, city, state, zip
                
                - Return ONLY valid JSON. No explanation, no markdown code fences, no preamble.
                
                Expected output format:
                {
                  "headerFields": {
                    "agency": "3200057",
                    "contactName": "Test",
                    "policyNumber": ""
                  },
                  "tables": [
                    {
                      "tableIndex": 1,
                      "rows": [
                        {
                          "classCode": "8585",
                          "locationNumber": "1",
                          "buildingNumber": "1",
                          "descriptionOfProperty": "personal property",
                          "street": "15565 County Rd #517",
                          "city": "Dexter",
                          "state": "MO",
                          "zip": "63841",
                          "valuation": "test",
                          "subject": "testing",
                          "100Values": "2",
                          "rateOrLossCost": "",
                          "premium": "1000"
                        },
                        {
                          "classCode": "",
                          "locationNumber": "1",
                          "buildingNumber": "2",
                          "descriptionOfProperty": "personal property",
                          "street": "15565 County Rd #517",
                          "city": "Dexter",
                          "state": "MO",
                          "zip": "63841",
                          "valuation": "te",
                          "subject": "",
                          "100Values": "",
                          "rateOrLossCost": "",
                          "premium": ""
                        }
                      ]
                    }
                  ]
                }
                """;
    
}
