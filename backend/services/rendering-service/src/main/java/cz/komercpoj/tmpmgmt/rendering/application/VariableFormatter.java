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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Formats variable values for output. Format strings follow {@code type[:pattern]}:
 *
 * <ul>
 *   <li>{@code number[:decimals]} — 1234.5 → {@code 1 234,50}
 *   <li>{@code currency[:code]} — 1234.5 → {@code 1 234,50 Kč} (default code from settings)
 *   <li>{@code date[:pattern]} — "2026-04-23" → {@code 23.04.2026}
 *   <li>{@code datetime[:pattern]} — Instant → {@code 23.04.2026 15:30}
 * </ul>
 *
 * <p>Per-call resolution order for locale / timezone / currency:
 *
 * <ol>
 *   <li>{@code X-Locale}, {@code X-Timezone}, {@code X-Currency} request headers — let multi-tenant
 *       callers pick a locale per request.
 *   <li>{@link AppSettingsCache} — admin-managed singleton from admin-service (5-min TTL),
 *       reflected immediately when the admin updates settings.
 *   <li>{@link RenderingProperties} — process-level fallback baked into config, used only when no
 *       cache is wired (tests) or admin-service has never returned a snapshot.
 * </ol>
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

  private final FormatterSettings serverDefaults;
  private AppSettingsCache cache;

  public VariableFormatter(RenderingProperties props) {
    this.serverDefaults =
        new FormatterSettings(
            Locale.forLanguageTag(props.locale()),
            ZoneId.of(props.timezone()),
            props.defaultCurrency());
  }

  /** Convenience for tests — uses cs_CZ / Europe/Prague / CZK. */
  public VariableFormatter() {
    this(new RenderingProperties(null, null, null, null, null));
  }

  /**
   * Setter-injected so production gets the persisted defaults via {@link AppSettingsCache}, while
   * unit tests using the bare constructor keep the existing process-level fallback unchanged.
   */
  @Autowired(required = false)
  public void setCache(AppSettingsCache cache) {
    this.cache = cache;
  }

  public String format(Object value, String format) {
    if (value == null) return "";
    if (format == null || format.isBlank()) return String.valueOf(value);

    String[] parts = format.split(":", 2);
    String type = parts[0];
    String pattern = parts.length > 1 ? parts[1] : null;

    FormatterSettings s = settings();
    return switch (type) {
      case "number" -> formatNumber(value, pattern, s);
      case "currency" -> formatCurrency(value, pattern, s);
      case "date" -> formatDate(value, pattern, false, s);
      case "datetime" -> formatDate(value, pattern, true, s);
      default -> String.valueOf(value);
    };
  }

  private String formatNumber(Object v, String decimalsStr, FormatterSettings s) {
    if (!(v instanceof Number n)) return String.valueOf(v);
    int decimals = parseDecimals(decimalsStr, 2);
    NumberFormat nf = NumberFormat.getNumberInstance(s.locale());
    nf.setMinimumFractionDigits(decimals);
    nf.setMaximumFractionDigits(decimals);
    return nf.format(n.doubleValue());
  }

  private String formatCurrency(Object v, String currencyCode, FormatterSettings s) {
    if (!(v instanceof Number n)) return String.valueOf(v);
    NumberFormat nf = NumberFormat.getCurrencyInstance(s.locale());
    try {
      nf.setCurrency(
          Currency.getInstance(
              currencyCode == null || currencyCode.isBlank() ? s.currency() : currencyCode));
    } catch (IllegalArgumentException e) {
      // unknown currency code — keep locale default
    }
    return nf.format(n.doubleValue());
  }

  private String formatDate(Object v, String pattern, boolean withTime, FormatterSettings s) {
    String p =
        (pattern == null || pattern.isBlank())
            ? (withTime ? DEFAULT_DATETIME_PATTERN : DEFAULT_DATE_PATTERN)
            : pattern;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(p).withLocale(s.locale());

    try {
      if (v instanceof LocalDate ld) return ld.format(dtf);
      if (v instanceof LocalDateTime ldt) return ldt.format(dtf);
      if (v instanceof Instant inst) return ZonedDateTime.ofInstant(inst, s.zone()).format(dtf);
      if (v instanceof String str) {
        // Try Instant (ISO 8601 with time) first, then LocalDate.
        try {
          return ZonedDateTime.ofInstant(Instant.parse(str), s.zone()).format(dtf);
        } catch (Exception ignored) {
          return LocalDate.parse(str).format(dtf);
        }
      }
    } catch (Exception e) {
      // Fallthrough → stringify
    }
    return String.valueOf(v);
  }

  private FormatterSettings settings() {
    FormatterSettings fallback = (cache != null) ? cache.get() : serverDefaults;
    HttpServletRequest req = currentRequest();
    if (req == null) {
      return fallback;
    }
    return new FormatterSettings(
        resolveLocale(req.getHeader(LOCALE_HEADER), fallback.locale()),
        resolveZone(req.getHeader(TIMEZONE_HEADER), fallback.zone()),
        resolveCurrency(req.getHeader(CURRENCY_HEADER), fallback.currency()));
  }

  private Locale resolveLocale(String header, Locale fallback) {
    if (header == null || header.isBlank()) return fallback;
    Locale parsed = Locale.forLanguageTag(header);
    String lang = parsed.getLanguage();
    // forLanguageTag is permissive: it stores arbitrary 2-3 letter language strings
    // (e.g. "not" from "not-a-real-tag") even though NumberFormat then silently
    // degrades to JVM root locale. Validate against the ISO 639-1 list explicitly.
    if (lang.isEmpty() || !ISO_LANGUAGES.contains(lang)) {
      return fallback;
    }
    String country = parsed.getCountry();
    if (!country.isEmpty() && !ISO_COUNTRIES.contains(country)) {
      return fallback;
    }
    return parsed;
  }

  private ZoneId resolveZone(String header, ZoneId fallback) {
    if (header == null || header.isBlank()) return fallback;
    try {
      return ZoneId.of(header);
    } catch (DateTimeException e) {
      return fallback;
    }
  }

  private String resolveCurrency(String header, String fallback) {
    if (header == null || header.isBlank()) return fallback;
    try {
      return Currency.getInstance(header).getCurrencyCode();
    } catch (IllegalArgumentException e) {
      return fallback;
    }
  }

  private static HttpServletRequest currentRequest() {
    var attrs = RequestContextHolder.getRequestAttributes();
    return (attrs instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
  }

  private static int parseDecimals(String s, int fallback) {
    if (s == null || s.isBlank()) return fallback;
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
