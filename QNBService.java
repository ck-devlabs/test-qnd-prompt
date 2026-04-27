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
}
