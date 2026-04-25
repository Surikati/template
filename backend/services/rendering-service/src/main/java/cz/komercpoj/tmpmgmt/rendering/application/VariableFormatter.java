package cz.komercpoj.tmpmgmt.rendering.application;

import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Formats variable values for output. Format strings follow {@code type[:pattern]}:
 *
 * <ul>
 *   <li>{@code number[:decimals]}    — 1234.5 → {@code 1 234,50}
 *   <li>{@code currency[:code]}      — 1234.5 → {@code 1 234,50 Kč} (default code from config)
 *   <li>{@code date[:pattern]}       — "2026-04-23" → {@code 23.04.2026}
 *   <li>{@code datetime[:pattern]}   — Instant → {@code 23.04.2026 15:30}
 * </ul>
 *
 * <p>Locale, timezone and default currency come from {@link RenderingProperties} as the
 * server-wide default. Each call also looks at the current HTTP request (if any) for the
 * headers {@code X-Locale}, {@code X-Timezone}, {@code X-Currency} and uses them to override
 * the defaults — letting multi-tenant callers pick a locale per request without code changes.
 * Outside of a request scope (tests, async work) only the defaults are used.
 */
@Component
public class VariableFormatter {

    private static final String DEFAULT_DATE_PATTERN = "dd.MM.yyyy";
    private static final String DEFAULT_DATETIME_PATTERN = "dd.MM.yyyy HH:mm";

    static final String LOCALE_HEADER = "X-Locale";
    static final String TIMEZONE_HEADER = "X-Timezone";
    static final String CURRENCY_HEADER = "X-Currency";

    private static final Set<String> ISO_LANGUAGES = Set.of(Locale.getISOLanguages());
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    private final Locale defaultLocale;
    private final ZoneId defaultZone;
    private final String defaultCurrency;

    public VariableFormatter(RenderingProperties props) {
        this.defaultLocale = Locale.forLanguageTag(props.locale());
        this.defaultZone = ZoneId.of(props.timezone());
        this.defaultCurrency = props.defaultCurrency();
    }

    /** Convenience for tests — uses cs_CZ / Europe/Prague / CZK. */
    public VariableFormatter() {
        this(new RenderingProperties(null, null, null, null, null));
    }

    public String format(Object value, String format) {
        if (value == null) return "";
        if (format == null || format.isBlank()) return String.valueOf(value);

        String[] parts = format.split(":", 2);
        String type = parts[0];
        String pattern = parts.length > 1 ? parts[1] : null;

        Settings s = settings();
        return switch (type) {
            case "number" -> formatNumber(value, pattern, s);
            case "currency" -> formatCurrency(value, pattern, s);
            case "date" -> formatDate(value, pattern, false, s);
            case "datetime" -> formatDate(value, pattern, true, s);
            default -> String.valueOf(value);
        };
    }

    private String formatNumber(Object v, String decimalsStr, Settings s) {
        if (!(v instanceof Number n)) return String.valueOf(v);
        int decimals = parseDecimals(decimalsStr, 2);
        NumberFormat nf = NumberFormat.getNumberInstance(s.locale);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        return nf.format(n.doubleValue());
    }

    private String formatCurrency(Object v, String currencyCode, Settings s) {
        if (!(v instanceof Number n)) return String.valueOf(v);
        NumberFormat nf = NumberFormat.getCurrencyInstance(s.locale);
        try {
            nf.setCurrency(Currency.getInstance(
                    currencyCode == null || currencyCode.isBlank() ? s.currency : currencyCode));
        } catch (IllegalArgumentException e) {
            // unknown currency code — keep locale default
        }
        return nf.format(n.doubleValue());
    }

    private String formatDate(Object v, String pattern, boolean withTime, Settings s) {
        String p = (pattern == null || pattern.isBlank())
                ? (withTime ? DEFAULT_DATETIME_PATTERN : DEFAULT_DATE_PATTERN)
                : pattern;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(p).withLocale(s.locale);

        try {
            if (v instanceof LocalDate ld) return ld.format(dtf);
            if (v instanceof LocalDateTime ldt) return ldt.format(dtf);
            if (v instanceof Instant inst) return ZonedDateTime.ofInstant(inst, s.zone).format(dtf);
            if (v instanceof String str) {
                // Try Instant (ISO 8601 with time) first, then LocalDate.
                try {
                    return ZonedDateTime.ofInstant(Instant.parse(str), s.zone).format(dtf);
                } catch (Exception ignored) {
                    return LocalDate.parse(str).format(dtf);
                }
            }
        } catch (Exception e) {
            // Fallthrough → stringify
        }
        return String.valueOf(v);
    }

    private Settings settings() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return new Settings(defaultLocale, defaultZone, defaultCurrency);
        }
        return new Settings(
                resolveLocale(req.getHeader(LOCALE_HEADER)),
                resolveZone(req.getHeader(TIMEZONE_HEADER)),
                resolveCurrency(req.getHeader(CURRENCY_HEADER)));
    }

    private Locale resolveLocale(String header) {
        if (header == null || header.isBlank()) return defaultLocale;
        Locale parsed = Locale.forLanguageTag(header);
        String lang = parsed.getLanguage();
        // forLanguageTag is permissive: it stores arbitrary 2-3 letter language strings
        // (e.g. "not" from "not-a-real-tag") even though NumberFormat then silently
        // degrades to JVM root locale. Validate against the ISO 639-1 list explicitly.
        if (lang.isEmpty() || !ISO_LANGUAGES.contains(lang)) {
            return defaultLocale;
        }
        String country = parsed.getCountry();
        if (!country.isEmpty() && !ISO_COUNTRIES.contains(country)) {
            return defaultLocale;
        }
        return parsed;
    }

    private ZoneId resolveZone(String header) {
        if (header == null || header.isBlank()) return defaultZone;
        try {
            return ZoneId.of(header);
        } catch (DateTimeException e) {
            return defaultZone;
        }
    }

    private String resolveCurrency(String header) {
        if (header == null || header.isBlank()) return defaultCurrency;
        try {
            return Currency.getInstance(header).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            return defaultCurrency;
        }
    }

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return (attrs instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
    }

    private static int parseDecimals(String s, int fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return fallback; }
    }

    private record Settings(Locale locale, ZoneId zone, String currency) {}
}
