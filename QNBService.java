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
        private static final DateTimeFormatter[] PARTIAL_DATE_FORMATTERS = {
    // 1. Try Full Month Names first (MMMM)
    new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM").toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d").toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM dd").toFormatter(Locale.ENGLISH),

    // 2. Try Abbreviated Month Names second (MMM)
    new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM").toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d").toFormatter(Locale.ENGLISH),
    new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM dd").toFormatter(Locale.ENGLISH)
};
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

    public MonthDay parsePartialDate(String rawDate) {
    // Remove ordinal suffixes: "17th" → "17"
    String normalized = rawDate.replaceAll("(?i)(?<=\\d)(st|nd|rd|th)\\b", "").trim();

    // Remove "of": "17 of April" → "17 April"
    normalized = normalized.replaceAll("(?i)\\bof\\b", "").trim();

    // Collapse multiple spaces into one
    normalized = normalized.replaceAll("\\s+", " ").trim();

    for (DateTimeFormatter formatter : PARTIAL_DATE_FORMATTERS) {
        try {
            return MonthDay.from(formatter.parse(normalized)); // ← key change
        } catch (Exception ignored) {
            // Try the next formatter
        }
    }
    throw new IllegalArgumentException(
        "Unable to parse partial date: '" + rawDate + "'. Expected formats: '9 Oct', '17th April', '17th of April'"
    );
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
}
