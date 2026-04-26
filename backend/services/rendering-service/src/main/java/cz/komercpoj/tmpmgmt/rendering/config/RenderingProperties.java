package cz.komercpoj.tmpmgmt.rendering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Locale + PDF font configuration. {@code cs_CZ} / {@code Europe/Prague} are server-wide defaults;
 * per-request callers can override via {@code X-Locale}, {@code X-Timezone}, {@code X-Currency}
 * headers — see {@link cz.komercpoj.tmpmgmt.rendering.application.VariableFormatter}.
 *
 * <p>PDF Czech diacritics need a Unicode TTF — PDFBox's built-in Type 1 fonts cover Latin-1 only.
 * Three resolution paths in priority order:
 *
 * <ol>
 *   <li>{@code pdfFontPath} — absolute filesystem path to a TTF the deployer bundled at deploy time
 *       (e.g. mounted volume, baked into the container image).
 *   <li>{@code pdfFontClasspath} — classpath resource path (e.g. {@code fonts/DejaVuSans.ttf}) so a
 *       deployer can ship the font as a regular Maven dependency or add it to a downstream custom
 *       image build.
 *   <li>OS auto-discovery — common Linux locations for DejaVu and Noto are probed at startup so the
 *       default {@code eclipse-temurin:21-jre-jammy} image works out of the box once the deployer
 *       adds {@code apt-get install fonts-dejavu}.
 * </ol>
 *
 * <p>This repo intentionally does not commit a binary font asset; deployers pick whichever path
 * fits their packaging.
 */
@ConfigurationProperties(prefix = "tmpmgmt.rendering")
public record RenderingProperties(
    String locale,
    String timezone,
    String defaultCurrency,
    String pdfFontPath,
    String pdfFontFamily,
    String pdfFontClasspath) {

  public RenderingProperties {
    if (locale == null || locale.isBlank()) locale = "cs-CZ";
    if (timezone == null || timezone.isBlank()) timezone = "Europe/Prague";
    if (defaultCurrency == null || defaultCurrency.isBlank()) defaultCurrency = "CZK";
    if (pdfFontFamily == null || pdfFontFamily.isBlank()) pdfFontFamily = "DejaVu Sans";
    // pdfFontPath, pdfFontClasspath stay nullable — null means "skip this lookup"
  }
}
