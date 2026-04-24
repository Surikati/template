package cz.komercpoj.tmpmgmt.rendering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Locale-sensitive defaults for the rendering pipeline. {@code cs_CZ} / {@code Europe/Prague}
 * matches MVP single-tenant deployment; multi-tenant rollouts should override per-request
 * (via JWT claim or {@code X-Locale} header) — see {@code VariableFormatter} TODO.
 */
@ConfigurationProperties(prefix = "tmpmgmt.rendering")
public record RenderingProperties(String locale, String timezone, String defaultCurrency) {

    public RenderingProperties {
        if (locale == null || locale.isBlank()) locale = "cs-CZ";
        if (timezone == null || timezone.isBlank()) timezone = "Europe/Prague";
        if (defaultCurrency == null || defaultCurrency.isBlank()) defaultCurrency = "CZK";
    }
}
