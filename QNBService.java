import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class QNBService {

    private static final int DAYS_WINDOW = 182;

    // Formats to try for partial date parsing (e.g., "9 Oct", "Oct 9", "9 October")
    private static final DateTimeFormatter[] PARTIAL_DATE_FORMATTERS = {
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM")
        .toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMM d")           // handles "Oct 7"
        .toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMM dd")          // handles "Oct 07"
        .toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMMM")
        .toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMMM d")          // handles "October 7"
        .toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMMM dd")         // handles "October 07"
        .toFormatter(Locale.ENGLISH)
};

    /**
     * Extracts the quoteNeedByDate from a JsonNode and resolves the full date
     * by finding the closest future occurrence within 182 days from today.
     *
     * @param jsonNode the JsonNode containing "quoteNeedByDate"
     * @return resolved LocalDate within the 182-day window
     * @throws IllegalArgumentException if the date is missing, unparseable, or out of range
     */
    public LocalDate resolveQuoteNeedByDate(JsonNode jsonNode) {
        // Step 1: Extract the raw date string from the JSON
        if (jsonNode == null || !jsonNode.has("quoteNeedByDate")) {
            throw new IllegalArgumentException("JsonNode is missing 'quoteNeedByDate' field");
        }

        String rawDate = jsonNode.get("quoteNeedByDate").asText().trim();
        if (rawDate.isEmpty()) {
            throw new IllegalArgumentException("'quoteNeedByDate' field is empty");
        }

        // Step 2: Parse the partial date (e.g., "9 Oct") into a MonthDay
        MonthDay monthDay = parsePartialDate(rawDate);

        // Step 3: Resolve the full date within the 182-day window
        return resolveWithinWindow(monthDay);
    }

    /**
     * Tries multiple formatters to parse a partial date string into a MonthDay.
     */
    private MonthDay parsePartialDate(String rawDate) {
        for (DateTimeFormatter formatter : PARTIAL_DATE_FORMATTERS) {
            try {
                return MonthDay.parse(rawDate, formatter);
            } catch (Exception ignored) {
                // Try the next formatter
            }
        }
        throw new IllegalArgumentException(
            "Unable to parse partial date: '" + rawDate + "'. Expected formats: '9 Oct', 'Oct 9', '9 October'"
        );
    }

    /**
     * Resolves a MonthDay to a full LocalDate by checking:
     * 1. Same year as today → if within [today, today + 182 days], return it.
     * 2. Next year → if within [today, today + 182 days], return it.
     * 3. Otherwise, throw an exception.
     */
    private LocalDate resolveWithinWindow(MonthDay monthDay) {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(DAYS_WINDOW);

        // Try current year first
        LocalDate candidateThisYear = monthDay.atYear(today.getYear());
        if (!candidateThisYear.isBefore(today) && !candidateThisYear.isAfter(windowEnd)) {
            return candidateThisYear;
        }

        // Try next year
        LocalDate candidateNextYear = monthDay.atYear(today.getYear() + 1);
        if (!candidateNextYear.isBefore(today) && !candidateNextYear.isAfter(windowEnd)) {
            return candidateNextYear;
        }

        // Date is outside the 182-day window
        throw new IllegalArgumentException(
            "Resolved date " + monthDay + " is outside the 182-day window [" + today + " → " + windowEnd + "]"
        );
    }

    public JsonNode resolveAndOverrideQuoteNeedByDate(JsonNode jsonNode) {
    // Resolve the full date using existing logic
    LocalDate resolvedDate = resolveQuoteNeedByDate(jsonNode);

    // Cast to ObjectNode to allow mutation
    if (!(jsonNode instanceof ObjectNode)) {
        throw new IllegalArgumentException("JsonNode must be an ObjectNode to allow field override");
    }

    ObjectNode objectNode = (ObjectNode) jsonNode;

    // Override the quoteNeedByDate field with the resolved full date
    objectNode.put("quoteNeedByDate", resolvedDate.toString()); // e.g. "2026-10-09"

    return objectNode;
}
}
