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
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM").toFormatter(Locale.ENGLISH),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d").toFormatter(Locale.ENGLISH),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM dd").toFormatter(Locale.ENGLISH),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM").toFormatter(Locale.ENGLISH),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d").toFormatter(Locale.ENGLISH),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM dd").toFormatter(Locale.ENGLISH)
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

    private MonthDay parsePartialDate(String rawDate) {
        // Normalize: remove ordinals, "of", and collapse extra spaces
        String normalized = rawDate.replaceAll("(?i)(?<=\\d)(st|nd|rd|th)\\b", "")
                                   .replaceAll("(?i)\\bof\\b", "")
                                   .replaceAll("\\s+", " ")
                                   .trim();

        for (DateTimeFormatter formatter : PARTIAL_DATE_FORMATTERS) {
            try {
                return MonthDay.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next pattern
            }
        }
        throw new IllegalArgumentException(
            "Unable to parse partial date: '" + rawDate + "'. Expected formats like '9 Oct' or '17th of April'"
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

        // Check last year, current year, and next year to handle year-wrap 
        // (e.g., today is Dec, user wants Jan)
        for (int yearOffset = -1; yearOffset <= 1; yearOffset++) {
            LocalDate candidate = monthDay.atYear(baseDate.getYear() + yearOffset);
            long daysFromToday = ChronoUnit.DAYS.between(baseDate, candidate);

            // Logic: Must be today or in the future, and within the window
            if (daysFromToday >= 0 && daysFromToday <= DAYS_WINDOW) {
                if (daysFromToday < smallestDiff) {
                    smallestDiff = daysFromToday;
                    best = candidate;
                }
            }
        }

        if (best == null) {
            // Fallback: If no date fits the window, provide the absolute closest future occurrence
            return monthDay.atYear(baseDate.getYear() + (monthDay.getMonthValue() < baseDate.getMonthValue() ? 1 : 0));
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
