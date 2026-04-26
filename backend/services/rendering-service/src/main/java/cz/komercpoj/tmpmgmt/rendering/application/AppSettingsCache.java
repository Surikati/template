package cz.komercpoj.tmpmgmt.rendering.application;

import cz.komercpoj.tmpmgmt.rendering.client.AdminSettingsClient;
import cz.komercpoj.tmpmgmt.rendering.client.AdminSettingsClient.AppSettingsDto;
import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import jakarta.annotation.PostConstruct;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * In-memory cache of the persisted app-level locale defaults from admin-service.
 *
 * <p>{@link VariableFormatter} reads from this cache once per render call. The cache refreshes
 * itself every 5 minutes (Spring {@link Scheduled}); on the first call after startup it loads
 * lazily so a fresh instance never serves stale-by-default values, and on any client failure it
 * keeps the last known good snapshot — falling back to {@link RenderingProperties} only when no
 * snapshot has ever been loaded.
 */
@Component
public class AppSettingsCache {

  private static final Logger log = LoggerFactory.getLogger(AppSettingsCache.class);
  private static final Duration STALE_AFTER = Duration.ofMinutes(5);
  private static final Set<String> ISO_LANGUAGES = Set.of(Locale.getISOLanguages());
  private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

  private final AdminSettingsClient client;
  private final FormatterSettings fallback;

  private volatile FormatterSettings current;
  private volatile Instant fetchedAt = Instant.EPOCH;

  public AppSettingsCache(AdminSettingsClient client, RenderingProperties props) {
    this.client = client;
    this.fallback = settingsFromProps(props);
    this.current = this.fallback;
  }

  @PostConstruct
  void primeOnStartup() {
    refresh();
  }

  /** Refresh on a 5-minute cadence. Failure keeps the last good snapshot. */
  @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT5M")
  void scheduledRefresh() {
    refresh();
  }

  /** Returns the current snapshot. Triggers a refresh if it's older than {@link #STALE_AFTER}. */
  public FormatterSettings get() {
    if (Duration.between(fetchedAt, Instant.now()).compareTo(STALE_AFTER) > 0) {
      refresh();
    }
    return current;
  }

  /** Visible-for-tests hook to swap the snapshot directly. */
  void overrideForTesting(FormatterSettings settings) {
    this.current = settings;
    this.fetchedAt = Instant.now();
  }

  private synchronized void refresh() {
    if (Duration.between(fetchedAt, Instant.now()).compareTo(STALE_AFTER) <= 0
        && current != fallback) {
      return; // someone else just refreshed
    }
    try {
      AppSettingsDto dto = client.fetch();
      this.current = parse(dto);
      this.fetchedAt = Instant.now();
    } catch (Exception e) {
      log.warn(
          "Failed to fetch app_settings from admin-service ({}). Keeping previous snapshot.",
          e.getMessage());
    }
  }

  private FormatterSettings settingsFromProps(RenderingProperties props) {
    return new FormatterSettings(
        Locale.forLanguageTag(props.locale()),
        ZoneId.of(props.timezone()),
        props.defaultCurrency());
  }

  private FormatterSettings parse(AppSettingsDto dto) {
    return new FormatterSettings(
        parseLocale(dto.locale()), parseZone(dto.timezone()), parseCurrency(dto.currency()));
  }

  private Locale parseLocale(String tag) {
    if (tag == null || tag.isBlank()) return fallback.locale();
    Locale parsed = Locale.forLanguageTag(tag);
    String lang = parsed.getLanguage();
    if (lang.isEmpty() || !ISO_LANGUAGES.contains(lang)) return fallback.locale();
    String country = parsed.getCountry();
    if (!country.isEmpty() && !ISO_COUNTRIES.contains(country)) return fallback.locale();
    return parsed;
  }

  private ZoneId parseZone(String zone) {
    if (zone == null || zone.isBlank()) return fallback.zone();
    try {
      return ZoneId.of(zone);
    } catch (DateTimeException e) {
      return fallback.zone();
    }
  }

  private String parseCurrency(String code) {
    if (code == null || code.isBlank()) return fallback.currency();
    try {
      return Currency.getInstance(code).getCurrencyCode();
    } catch (IllegalArgumentException e) {
      return fallback.currency();
    }
  }
}
