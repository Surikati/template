package cz.komercpoj.tmpmgmt.rendering.application;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Formats variable values for output. Format strings follow {@code type[:pattern]}:
 *
 * <ul>
 *   <li>{@code number[:decimals]}    — 1234.5 → {@code 1 234,50}
 *   <li>{@code currency[:code]}      — 1234.5 → {@code 1 234,50 Kč} (default code CZK)
 *   <li>{@code date[:pattern]}       — "2026-04-23" → {@code 23.04.2026}
 *   <li>{@code datetime[:pattern]}   — Instant → {@code 23.04.2026 15:30}
 * </ul>
 *
 * <p>Unknown or missing format → value stringified via {@link String#valueOf}.
 */
@Component
public class VariableFormatter {

    private static final Locale LOCALE = Locale.of("cs", "CZ");
    private static final ZoneId ZONE = ZoneId.of("Europe/Prague");
    private static final String DEFAULT_DATE_PATTERN = "dd.MM.yyyy";
    private static final String DEFAULT_DATETIME_PATTERN = "dd.MM.yyyy HH:mm";
    private static final String DEFAULT_CURRENCY = "CZK";

    public String format(Object value, String format) {
        if (value == null) return "";
        if (format == null || format.isBlank()) return String.valueOf(value);

        String[] parts = format.split(":", 2);
        String type = parts[0];
        String pattern = parts.length > 1 ? parts[1] : null;

        return switch (type) {
            case "number" -> formatNumber(value, pattern);
            case "currency" -> formatCurrency(value, pattern);
            case "date" -> formatDate(value, pattern, false);
            case "datetime" -> formatDate(value, pattern, true);
            default -> String.valueOf(value);
        };
    }

    private String formatNumber(Object v, String decimalsStr) {
        if (!(v instanceof Number n)) return String.valueOf(v);
        int decimals = parseDecimals(decimalsStr, 2);
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        return nf.format(n.doubleValue());
    }

    private String formatCurrency(Object v, String currencyCode) {
        if (!(v instanceof Number n)) return String.valueOf(v);
        NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE);
        try {
            nf.setCurrency(Currency.getInstance(
                    currencyCode == null || currencyCode.isBlank() ? DEFAULT_CURRENCY : currencyCode));
        } catch (IllegalArgumentException e) {
            // unknown currency code — keep locale default
        }
        return nf.format(n.doubleValue());
    }

    private String formatDate(Object v, String pattern, boolean withTime) {
        String p = (pattern == null || pattern.isBlank())
                ? (withTime ? DEFAULT_DATETIME_PATTERN : DEFAULT_DATE_PATTERN)
                : pattern;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(p).withLocale(LOCALE);

        try {
            if (v instanceof LocalDate ld) return ld.format(dtf);
            if (v instanceof LocalDateTime ldt) return ldt.format(dtf);
            if (v instanceof Instant inst) return ZonedDateTime.ofInstant(inst, ZONE).format(dtf);
            if (v instanceof String s) {
                // Try Instant (ISO 8601 with time) first, then LocalDate.
                try {
                    return ZonedDateTime.ofInstant(Instant.parse(s), ZONE).format(dtf);
                } catch (Exception ignored) {
                    return LocalDate.parse(s).format(dtf);
                }
            }
        } catch (Exception e) {
            // Fallthrough → stringify
        }
        return String.valueOf(v);
    }

    private static int parseDecimals(String s, int fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return fallback; }
    }
}
