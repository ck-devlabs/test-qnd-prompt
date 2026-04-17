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

    private LocalDate resolveWithinWindow(MonthDay monthDay, LocalDate baseDate) {
    LocalDate best = null;
        long bestScore = Long.MAX_VALUE;

        for (int yearOffset = -1; yearOffset <= 1; yearOffset++) {
            LocalDate candidate = monthDay.atYear(baseDate.getYear() + yearOffset);

            long daysDiff = Math.abs(ChronoUnit.DAYS.between(baseDate, candidate));
            long score = Math.abs(daysDiff - 182);

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
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



// Matches: "17th of April", "31st April", "2nd of March", etc.
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "^(\\d{1,2})(?:st|nd|rd|th)\\s+(?:of\\s+)?(\\w+)$",
        Pattern.CASE_INSENSITIVE
    );

    private static final DateTimeFormatter OUTPUT_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);

    public String format(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Date input must not be null or blank");
        }

        Matcher matcher = DATE_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            throw new DateParseException("Unrecognized date format: " + input);
        }

        int day = Integer.parseInt(matcher.group(1));
        String monthStr = matcher.group(2);

        Month month = parseMonth(monthStr);
        validateDay(day, month);

        // Use a leap year (2000) to avoid Feb 29 false negatives
        LocalDate date = LocalDate.of(2000, month, day);
        return date.format(OUTPUT_FORMATTER);
    }

    private Month parseMonth(String monthStr) {
        try {
            return Month.valueOf(monthStr.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            throw new DateParseException("Unknown month: " + monthStr);
        }
    }

    private void validateDay(int day, Month month) {
        // Max days using a leap year to be lenient on Feb
        int maxDay = month.length(true);
        if (day < 1 || day > maxDay) {
            throw new DateParseException(
                String.format("Day %d is invalid for month %s", day, month)
            );
        }
    }






    
}
