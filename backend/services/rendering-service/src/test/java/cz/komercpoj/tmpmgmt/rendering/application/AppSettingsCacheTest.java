package cz.komercpoj.tmpmgmt.rendering.application;

import static org.assertj.core.api.Assertions.assertThat;

import cz.komercpoj.tmpmgmt.rendering.client.AdminSettingsClient;
import cz.komercpoj.tmpmgmt.rendering.client.AdminSettingsClient.AppSettingsDto;
import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class AppSettingsCacheTest {

  private static final RenderingProperties CS_DEFAULTS =
      new RenderingProperties(null, null, null, null, null);
  private static final RenderingProperties EN_DEFAULTS =
      new RenderingProperties("en-US", "America/New_York", "USD", null, null);

  @Test
  void primesFromAdminService_onConstruction() {
    AdminSettingsClient client = () -> new AppSettingsDto("fr-FR", "Europe/Paris", "EUR");
    AppSettingsCache cache = new AppSettingsCache(client, CS_DEFAULTS);
    cache.primeOnStartup();

    FormatterSettings s = cache.get();
    assertThat(s.locale()).isEqualTo(Locale.forLanguageTag("fr-FR"));
    assertThat(s.zone()).isEqualTo(ZoneId.of("Europe/Paris"));
    assertThat(s.currency()).isEqualTo("EUR");
  }

  @Test
  void fallsBackToProperties_whenAdminServiceUnavailableOnPrime() {
    AdminSettingsClient failing =
        () -> {
          throw new RuntimeException("admin-service down");
        };
    AppSettingsCache cache = new AppSettingsCache(failing, EN_DEFAULTS);
    cache.primeOnStartup();

    FormatterSettings s = cache.get();
    assertThat(s.locale()).isEqualTo(Locale.forLanguageTag("en-US"));
    assertThat(s.zone()).isEqualTo(ZoneId.of("America/New_York"));
    assertThat(s.currency()).isEqualTo("USD");
  }

  @Test
  void invalidServerValues_fallBackPerField_andKeepValidOnes() {
    AdminSettingsClient client = () -> new AppSettingsDto("not-a-real-tag", "Mars/Olympus", "ZZZ");
    AppSettingsCache cache = new AppSettingsCache(client, EN_DEFAULTS);
    cache.primeOnStartup();

    FormatterSettings s = cache.get();
    // None of the three values were valid → all three fall back to RenderingProperties
    assertThat(s.locale()).isEqualTo(Locale.forLanguageTag("en-US"));
    assertThat(s.zone()).isEqualTo(ZoneId.of("America/New_York"));
    assertThat(s.currency()).isEqualTo("USD");
  }

  @Test
  void overrideForTesting_replacesSnapshot() {
    AdminSettingsClient client = () -> new AppSettingsDto("cs-CZ", "Europe/Prague", "CZK");
    AppSettingsCache cache = new AppSettingsCache(client, CS_DEFAULTS);
    cache.primeOnStartup();

    cache.overrideForTesting(
        new FormatterSettings(Locale.forLanguageTag("ja-JP"), ZoneId.of("Asia/Tokyo"), "JPY"));
    FormatterSettings s = cache.get();
    assertThat(s.currency()).isEqualTo("JPY");
    assertThat(s.zone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
  }

  @Test
  void integratesWithVariableFormatter_asFallback_whenNoRequestBound() {
    AdminSettingsClient client = () -> new AppSettingsDto("en-US", "America/New_York", "USD");
    AppSettingsCache cache = new AppSettingsCache(client, CS_DEFAULTS);
    cache.primeOnStartup();

    VariableFormatter formatter = new VariableFormatter();
    formatter.setCache(cache);

    // No request bound → fallback to cache (en-US uses comma grouping, dot decimal).
    assertThat(formatter.format(1234.5, "number:2")).isEqualTo("1,234.50");
    assertThat(formatter.format(100, "currency")).contains("$");
  }
}
