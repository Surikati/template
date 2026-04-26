package cz.komercpoj.tmpmgmt.rendering.application;

import static org.assertj.core.api.Assertions.*;

import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class VariableFormatterTest {

  private final VariableFormatter formatter = new VariableFormatter();

  @AfterEach
  void clearRequestContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static void bindRequest(MockHttpServletRequest req) {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
  }

  @Test
  void nullValue_returnsEmptyString() {
    assertThat(formatter.format(null, null)).isEmpty();
    assertThat(formatter.format(null, "currency:CZK")).isEmpty();
  }

  @Test
  void noFormat_stringifiesValue() {
    assertThat(formatter.format("Jan Novák", null)).isEqualTo("Jan Novák");
    assertThat(formatter.format(42, null)).isEqualTo("42");
    assertThat(formatter.format(3.14, "")).isEqualTo("3.14");
  }

  @Test
  void numberWithDecimals() {
    // Czech locale uses non-breaking space as grouping separator, comma as decimal.
    String out = formatter.format(1234.5, "number:2");
    assertThat(out).contains("234").contains(",50").doesNotContain(".50");
    assertThat(formatter.format(1000, "number:0")).isEqualTo("1 000");
  }

  @Test
  void currencyWithExplicitCode() {
    String out = formatter.format(1234.5, "currency:CZK");
    // Czech locale: "1 234,50 Kč" — assert key parts
    assertThat(out).contains("1").contains("234").contains(",50").contains("Kč");
  }

  @Test
  void currencyDefault() {
    String out = formatter.format(100, "currency");
    assertThat(out).contains("100").contains("Kč");
  }

  @Test
  void dateFromIsoString_defaultPattern() {
    assertThat(formatter.format("2026-04-23", "date")).isEqualTo("23.04.2026");
  }

  @Test
  void dateFromIsoString_customPattern() {
    assertThat(formatter.format("2026-04-23", "date:d. M. yyyy")).isEqualTo("23. 4. 2026");
  }

  @Test
  void dateFromLocalDate() {
    assertThat(formatter.format(LocalDate.of(2026, 4, 23), "date:dd.MM.yyyy"))
        .isEqualTo("23.04.2026");
  }

  @Test
  void datetimeFromInstant() {
    Instant noon = Instant.parse("2026-04-23T10:00:00Z"); // Europe/Prague = UTC+2 = 12:00
    String out = formatter.format(noon, "datetime:dd.MM.yyyy HH:mm");
    assertThat(out).isEqualTo("23.04.2026 12:00");
  }

  @Test
  void unknownFormat_fallsBackToStringify() {
    assertThat(formatter.format(42, "bogus:pattern")).isEqualTo("42");
  }

  @Test
  void nonNumberWithCurrencyFormat_fallsBack() {
    assertThat(formatter.format("not a number", "currency:CZK")).isEqualTo("not a number");
  }

  @Test
  void englishLocale_usesEnglishGroupingAndDecimal() {
    VariableFormatter en =
        new VariableFormatter(
            new RenderingProperties("en-US", "America/New_York", "USD", null, null, null));
    // en-US: comma grouping, dot decimal
    assertThat(en.format(1234.5, "number:2")).isEqualTo("1,234.50");
  }

  @Test
  void englishLocaleDefaultCurrency_usesUsdSymbol() {
    VariableFormatter en =
        new VariableFormatter(
            new RenderingProperties("en-US", "America/New_York", "USD", null, null, null));
    String out = en.format(100, "currency");
    assertThat(out).contains("$").contains("100");
  }

  @Test
  void timezoneOverride_shiftsInstantPresentation() {
    VariableFormatter ny =
        new VariableFormatter(
            new RenderingProperties("en-US", "America/New_York", "USD", null, null, null));
    Instant noon = Instant.parse("2026-04-23T16:00:00Z"); // 12:00 EDT (UTC-4)
    String out = ny.format(noon, "datetime:yyyy-MM-dd HH:mm");
    assertThat(out).isEqualTo("2026-04-23 12:00");
  }

  @Test
  void xLocaleHeader_overridesServerDefault() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Locale", "en-US");
    bindRequest(req);
    // Server default is cs-CZ; en-US groups with comma, decimal with dot
    assertThat(formatter.format(1234.5, "number:2")).isEqualTo("1,234.50");
  }

  @Test
  void xTimezoneHeader_shiftsInstantPresentation() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Timezone", "America/New_York");
    bindRequest(req);
    Instant noon = Instant.parse("2026-04-23T16:00:00Z"); // 12:00 EDT (UTC-4)
    // Locale stays cs-CZ, only zone shifts
    assertThat(formatter.format(noon, "datetime:yyyy-MM-dd HH:mm")).isEqualTo("2026-04-23 12:00");
  }

  @Test
  void xCurrencyHeader_overridesDefaultCode() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Currency", "USD");
    bindRequest(req);
    // Without explicit code in format string, header takes effect
    String out = formatter.format(100, "currency");
    assertThat(out).contains("100").doesNotContain("Kč");
  }

  @Test
  void invalidXLocaleHeader_fallsBackToDefault() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Locale", "not-a-real-tag");
    bindRequest(req);
    // Falls back to cs-CZ → comma decimal (en-US would use dot)
    String out = formatter.format(1234.5, "number:2");
    assertThat(out).contains("234").contains(",50").doesNotContain(".50");
  }

  @Test
  void invalidXTimezoneHeader_fallsBackToDefault() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Timezone", "Mars/Olympus_Mons");
    bindRequest(req);
    Instant noon = Instant.parse("2026-04-23T10:00:00Z"); // Europe/Prague = 12:00
    assertThat(formatter.format(noon, "datetime:dd.MM.yyyy HH:mm")).isEqualTo("23.04.2026 12:00");
  }

  @Test
  void invalidXCurrencyHeader_fallsBackToDefault() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Currency", "ZZZ");
    bindRequest(req);
    // Falls back to CZK
    assertThat(formatter.format(100, "currency")).contains("Kč");
  }

  @Test
  void explicitFormatCurrencyCode_winsOverHeader() {
    var req = new MockHttpServletRequest();
    req.addHeader("X-Currency", "USD");
    bindRequest(req);
    // currency:CZK in format string takes precedence over X-Currency header
    assertThat(formatter.format(100, "currency:CZK")).contains("Kč");
  }
}
