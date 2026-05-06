package com.hanover.entp.dataext;

import com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentTable;
import com.azure.ai.formrecognizer.documentanalysis.models.DocumentTableCell;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to convert Azure Document Intelligence tables
 * into a GPT-ready string (Markdown format).
 *
 * Problems solved:
 *
 *   1. BLANK ROW OMISSION
 *      Azure Doc Intelligence omits entirely blank rows from getCells().
 *      getRowCount() still reflects the true PDF row count, so we allocate
 *      the full grid upfront (pre-filled with "") and only overwrite cells
 *      that were actually reported. Blank rows stay as all-"" rows.
 *
 *   2. TWO-PHYSICAL-ROWS-PER-RECORD PATTERN
 *      This form layout uses 2 Markdown rows per logical record:
 *        Row A: CLASS CODE | LOC # | BLDG # | Description | Valuation | Subject | 100% Values | Rate | Premium
 *        Row B:            |       |        | Address line |           |         |             |      |
 *      If both rows are sent to GPT separately, GPT drops Row B as a duplicate/blank.
 *      Fix: detect this pattern in Java and merge Row A + Row B into one row by
 *      concatenating the DESCRIPTION OF PROPERTY cell (which holds both description
 *      and address lines). GPT then receives one clean row per record and never
 *      has to reason about row pairing.
 */
public class DocumentTableExtractor {

    /**
     * Main entry point.
     * Combines all tables + raw text (non-table content) into a single
     * GPT-ready user message string.
     *
     * @param result  AnalyzeResult from Azure Doc Intelligence poller.getFinalResult()
     * @return        Formatted string ready to be sent to GPT as the user message
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
     *
     * Step 1 — Build full grid (blank rows preserved via pre-fill).
     * Step 2 — Detect and merge 2-row-per-record pattern before serializing.
     *
     * @param table  DocumentTable from AnalyzeResult
     * @return       Markdown-formatted table string
     */
    public static String tableToMarkdown(DocumentTable table) {
        int rowCount = table.getRowCount();
        int colCount = table.getColumnCount();

        // Step 1: Allocate full grid pre-filled with ""
        // Blank rows exist here even if getCells() never reported them
        String[][] grid = new String[rowCount][colCount];
        for (String[] row : grid) {
            Arrays.fill(row, "");
        }

        // Track which rows have at least one cell reported (for debug logging)
        Set<Integer> rowsWithContent = new HashSet<>();

        // Fill only the cells Doc Intelligence reported
        for (DocumentTableCell cell : table.getCells()) {
            int row = cell.getRowIndex();
            int col = cell.getColumnIndex();
            if (row < rowCount && col < colCount) {
                String content = cell.getContent();
                grid[row][col] = (content != null) ? content.trim() : "";
                rowsWithContent.add(row);
            }
        }

        // Log blank rows for debugging
        for (int r = 0; r < rowCount; r++) {
            if (!rowsWithContent.contains(r)) {
                System.out.printf("[DocumentTableExtractor] Row %d fully blank " +
                        "(no cells from Doc Intelligence) — preserved as empty row%n", r);
            }
        }

        // Step 2: Merge 2-physical-rows-per-record into 1 logical row
        // This prevents GPT from dropping the address sub-row
        String[][] mergedGrid = mergeRowPairs(grid);

        return gridToMarkdown(mergedGrid);
    }

    /**
     * Detects and merges the 2-physical-rows-per-record pattern common in
     * insurance forms like ACORD schedules.
     *
     * Pattern:
     *   Row A (odd data row):  CLASS CODE | LOC # | BLDG # | "personal property" | ...values...
     *   Row B (even data row): ""         | ""    | ""     | "15565 County Rd #517 Dexter MO 63841" | ""...
     *
     * Detection heuristic: Row B is a sub-row if:
     *   - LOC # and BLDG # columns are blank
     *   - At least one other column (description/address col) has content
     *   - Row A immediately above had content in LOC # or BLDG #
     *
     * Merge strategy: append Row B's description/address cell content to
     * Row A's description cell with a newline, so GPT receives the full
     * address inline with the record.
     *
     * If the pattern is NOT detected (different form layout), the grid is
     * returned unchanged — no data is lost.
     *
     * @param grid  Full grid with blank rows preserved
     * @return      Merged grid with one row per logical record
     */
    private static String[][] mergeRowPairs(String[][] grid) {
        if (grid.length < 2) return grid;

        // Identify header row(s) — rows where LOC # and BLDG # cells contain
        // header text (non-numeric). Typically row 0 or rows 0-1 for multi-line headers.
        // We skip header rows and only merge data rows.
        int firstDataRow = findFirstDataRow(grid);

        // Detect the pattern: check if row (firstDataRow + 1) looks like a sub-row
        if (!isSubRowPattern(grid, firstDataRow)) {
            System.out.println("[DocumentTableExtractor] No 2-row-per-record pattern detected — grid unchanged");
            return grid;
        }

        System.out.println("[DocumentTableExtractor] 2-row-per-record pattern detected — merging row pairs");

        // Find which column index is the description/address column
        // (column with the most multi-line content across sub-rows)
        int descColIndex = findDescriptionColumnIndex(grid, firstDataRow);

        // Build merged grid: header rows + one merged row per data pair
        int headerRowCount = firstDataRow;
        int dataRowCount = grid.length - headerRowCount;
        int mergedDataRows = (int) Math.ceil(dataRowCount / 2.0);
        int colCount = grid[0].length;

        String[][] merged = new String[headerRowCount + mergedDataRows][colCount];

        // Copy header rows unchanged
        for (int r = 0; r < headerRowCount; r++) {
            merged[r] = Arrays.copyOf(grid[r], colCount);
        }

        // Merge data row pairs
        int mergedRow = headerRowCount;
        for (int r = firstDataRow; r < grid.length; r += 2) {
            String[] rowA = grid[r];
            String[] rowB = (r + 1 < grid.length) ? grid[r + 1] : new String[colCount];

            String[] combinedRow = Arrays.copyOf(rowA, colCount);

            // Append Row B's description/address content to Row A's description cell
            String addressContent = rowB[descColIndex].trim();
            if (!addressContent.isEmpty()) {
                String existing = combinedRow[descColIndex].trim();
                combinedRow[descColIndex] = existing.isEmpty()
                        ? addressContent
                        : existing + "\n" + addressContent;
            }

            // Also merge any other non-blank cells from Row B that Row A left blank
            for (int c = 0; c < colCount; c++) {
                if (c != descColIndex && combinedRow[c].isEmpty() && !rowB[c].isEmpty()) {
                    combinedRow[c] = rowB[c];
                }
            }

            merged[mergedRow++] = combinedRow;
        }

        return merged;
    }

    /**
     * Finds the first data row index (skips header rows).
     * A header row is one where the LOC # column contains non-numeric text
     * or the entire row has no numeric values.
     */
    private static int findFirstDataRow(String[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (String cell : grid[r]) {
                if (cell.matches("\\d+")) return r; // first row with a numeric cell = first data row
            }
        }
        return 1; // fallback: assume row 0 is header
    }

    /**
     * Detects whether the table uses a 2-physical-rows-per-record pattern.
     * Checks if the row immediately after the first data row has blank LOC #
     * and BLDG # columns (columns 1 and 2 in ACORD layout) but has content
     * in the description column.
     */
    private static boolean isSubRowPattern(String[][] grid, int firstDataRow) {
        int subRowIndex = firstDataRow + 1;
        if (subRowIndex >= grid.length) return false;

        String[] subRow = grid[subRowIndex];
        if (subRow.length < 3) return false;

        // LOC # (col 1) and BLDG # (col 2) should be blank in a sub-row
        boolean locBlank  = subRow[1].isEmpty();
        boolean bldgBlank = subRow[2].isEmpty();

        // At least one cell in the sub-row must have content (the address)
        boolean hasAnyContent = Arrays.stream(subRow).anyMatch(c -> !c.isEmpty());

        return locBlank && bldgBlank && hasAnyContent;
    }

    /**
     * Finds the column index most likely to contain the description/address text.
     * Looks for the column with the longest average content in sub-rows
     * (every other data row starting at firstDataRow + 1).
     */
    private static int findDescriptionColumnIndex(String[][] grid, int firstDataRow) {
        int colCount = grid[0].length;
        int[] contentLength = new int[colCount];

        for (int r = firstDataRow + 1; r < grid.length; r += 2) {
            for (int c = 0; c < Math.min(colCount, grid[r].length); c++) {
                contentLength[c] += grid[r][c].length();
            }
        }

        int maxCol = 3; // default to column 3 (description column in ACORD layout)
        int maxLen = 0;
        for (int c = 0; c < colCount; c++) {
            if (contentLength[c] > maxLen) {
                maxLen = contentLength[c];
                maxCol = c;
            }
        }
        return maxCol;
    }

    /**
     * Converts a 2D String grid to a Markdown table.
     * Row 0 is treated as the header row and gets a separator line.
     *
     * @param grid  2D array of cell values
     * @return      Markdown table string
     */
    private static String gridToMarkdown(String[][] grid) {
        if (grid.length == 0) return "(empty table)\n";

        StringBuilder sb = new StringBuilder();

        for (int r = 0; r < grid.length; r++) {
            sb.append("| ");
            sb.append(String.join(" | ", grid[r]));
            sb.append(" |\n");

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
     * Builds the GPT system prompt.
     * Focuses purely on extraction rules — output structure and schema
     * are enforced separately via OpenAI responseFormat / JSON schema.
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

                GENERAL EXTRACTION RULES:
                - Extract ALL fields from the header section into headerFields
                - Extract ALL rows from every table — including rows where every cell is blank
                - A blank row is a valid record and must appear in the output with all fields as ""
                - Preserve empty string "" for ALL blank cells — do NOT skip or omit any row or cell
                - If no clear header row exists, use column keys: col_0, col_1, col_2, etc.

                LOCATION AND BUILDING NUMBER RULES:
                - "locationNumber" maps to the LOC # column (also written as "Loc #", "Location #", "Loc No", or equivalent)
                - "buildingNumber" maps to the BLDG # column (also written as "Bldg #", "Building #", "Bldg No", or equivalent)
                - If a locationNumber or buildingNumber cell is blank, inherit the last non-blank value
                  seen above it in the same table column — do NOT leave it blank if a prior row had a value
                - If no value exists anywhere in the column, use ""

                ADDRESS PARSING RULES:
                - Each record's description cell may contain multiple lines — the first line is the
                  property description, subsequent lines are the address. For example:
                    "personal property\\n15565 County Rd #517\\nDexter MO 63841"
                  Parse the address lines into:
                    street  - street number and name only (e.g. "15565 County Rd #517")
                    city    - city name only (e.g. "Dexter")
                    state   - 2-letter state code only (e.g. "MO")
                    zip     - ZIP or ZIP+4 code only (e.g. "63841")
                - If any address component cannot be determined, use ""
                """;
    }

    // -------------------------------------------------------------------------
    // Example usage (remove or move to a service class in production)
    // -------------------------------------------------------------------------

    /**
     * Example: how to wire this into your existing service.
     *
     * AnalyzeResult result = poller.getFinalResult();
     *
     * String systemPrompt = DocumentTableExtractor.buildSystemPrompt();
     * String userMessage  = DocumentTableExtractor.buildGptInput(result);
     *
     * // Pass to OpenAI with responseFormat enforcing your JSON schema:
     * ChatCompletionRequest request = ChatCompletionRequest.builder()
     *     .model("gpt-4o")
     *     .messages(List.of(
     *         new SystemMessage(systemPrompt),
     *         new UserMessage(userMessage)
     *     ))
     *     .responseFormat(ResponseFormat.builder()
     *         .type("json_schema")
     *         .jsonSchema(yourSchema)
     *         .build())
     *     .build();
     *
     * String jsonResponse = openAiService.createChatCompletion(request)
     *     .getChoices().get(0).getMessage().getContent();
     */
}
