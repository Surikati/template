package cz.komercpoj.tmpmgmt.rendering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Locale + PDF font configuration. {@code cs_CZ} / {@code Europe/Prague} are server-wide
 * defaults; per-request callers can override via {@code X-Locale}, {@code X-Timezone},
 * {@code X-Currency} headers — see {@link cz.komercpoj.tmpmgmt.rendering.application.VariableFormatter}.
 *
 * <p>{@code pdfFontPath} points to a Unicode TTF (e.g. DejaVuSans, NotoSans) bundled by the
 * deployer — this repo intentionally does not ship a binary font asset. If unset, PDF
 * generation falls back to PDFBox built-in fonts, which lack Czech diacritics support.
 */
@ConfigurationProperties(prefix = "tmpmgmt.rendering")
public record RenderingProperties(
        String locale,
        String timezone,
        String defaultCurrency,
        String pdfFontPath,
        String pdfFontFamily) {

    public RenderingProperties {
        if (locale == null || locale.isBlank()) locale = "cs-CZ";
        if (timezone == null || timezone.isBlank()) timezone = "Europe/Prague";
        if (defaultCurrency == null || defaultCurrency.isBlank()) defaultCurrency = "CZK";
        if (pdfFontFamily == null || pdfFontFamily.isBlank()) pdfFontFamily = "DejaVu Sans";
        // pdfFontPath stays nullable — null means "use PDFBox defaults"
    }
}
