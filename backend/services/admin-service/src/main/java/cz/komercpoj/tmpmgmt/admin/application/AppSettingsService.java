package cz.komercpoj.tmpmgmt.admin.application;

import cz.komercpoj.tmpmgmt.admin.persistence.AppSettingsEntity;
import cz.komercpoj.tmpmgmt.admin.persistence.AppSettingsRepository;
import cz.komercpoj.tmpmgmt.common.ValidationException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and updates the singleton {@code app_settings} row. */
@Service
public class AppSettingsService {

  private static final Set<String> ISO_LANGUAGES = Set.of(Locale.getISOLanguages());
  private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

  private final AppSettingsRepository repo;

  public AppSettingsService(AppSettingsRepository repo) {
    this.repo = repo;
  }

  @Transactional(readOnly = true)
  public AppSettingsEntity get() {
    return repo.findById(AppSettingsEntity.SINGLETON_ID)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "app_settings singleton row missing — V2 migration didn't run"));
  }

  @Transactional
  public AppSettingsEntity update(String locale, String timezone, String currency, UUID actor) {
    List<String> violations = new ArrayList<>();
    validateLocale(locale, violations);
    validateTimezone(timezone, violations);
    validateCurrency(currency, violations);
    if (!violations.isEmpty()) {
      throw new ValidationException(
          "settings.invalid", "App settings payload is invalid", violations);
    }
    AppSettingsEntity entity = get();
    entity.update(locale, timezone, normaliseCurrency(currency), actor);
    return entity;
  }

  private static void validateLocale(String tag, List<String> violations) {
    if (tag == null || tag.isBlank()) {
      violations.add("locale: must not be blank");
      return;
    }
    Locale parsed = Locale.forLanguageTag(tag);
    String lang = parsed.getLanguage();
    if (lang.isEmpty() || !ISO_LANGUAGES.contains(lang)) {
      violations.add("locale: not a recognised ISO 639-1 language tag (" + tag + ")");
      return;
    }
    String country = parsed.getCountry();
    if (!country.isEmpty() && !ISO_COUNTRIES.contains(country)) {
      violations.add("locale: not a recognised ISO 3166-1 country (" + tag + ")");
    }
  }

  private static void validateTimezone(String zone, List<String> violations) {
    if (zone == null || zone.isBlank()) {
      violations.add("timezone: must not be blank");
      return;
    }
    try {
      ZoneId.of(zone);
    } catch (DateTimeException e) {
      violations.add("timezone: not a recognised IANA zone id (" + zone + ")");
    }
  }

  private static void validateCurrency(String code, List<String> violations) {
    if (code == null || code.isBlank()) {
      violations.add("currency: must not be blank");
      return;
    }
    try {
      Currency.getInstance(code.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      violations.add("currency: not a recognised ISO 4217 code (" + code + ")");
    }
  }

  private static String normaliseCurrency(String code) {
    return code.toUpperCase(Locale.ROOT);
  }
}
