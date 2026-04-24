package cz.komercpoj.tmpmgmt.rendering.application;

import static org.assertj.core.api.Assertions.*;

import cz.komercpoj.tmpmgmt.rendering.config.RenderingProperties;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class VariableFormatterTest {

    private final VariableFormatter formatter = new VariableFormatter();

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
        assertThat(formatter.format(1234.5, "number:2")).isEqualTo("1 234,50");
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
        Instant noon = Instant.parse("2026-04-23T10:00:00Z");  // Europe/Prague = UTC+2 = 12:00
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
        VariableFormatter en = new VariableFormatter(
                new RenderingProperties("en-US", "America/New_York", "USD", null, null));
        // en-US: comma grouping, dot decimal
        assertThat(en.format(1234.5, "number:2")).isEqualTo("1,234.50");
    }

    @Test
    void englishLocaleDefaultCurrency_usesUsdSymbol() {
        VariableFormatter en = new VariableFormatter(
                new RenderingProperties("en-US", "America/New_York", "USD", null, null));
        String out = en.format(100, "currency");
        assertThat(out).contains("$").contains("100");
    }

    @Test
    void timezoneOverride_shiftsInstantPresentation() {
        VariableFormatter ny = new VariableFormatter(
                new RenderingProperties("en-US", "America/New_York", "USD", null, null));
        Instant noon = Instant.parse("2026-04-23T16:00:00Z"); // 12:00 EDT (UTC-4)
        String out = ny.format(noon, "datetime:yyyy-MM-dd HH:mm");
        assertThat(out).isEqualTo("2026-04-23 12:00");
    }
}
