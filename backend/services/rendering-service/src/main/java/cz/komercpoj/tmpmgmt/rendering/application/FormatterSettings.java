package cz.komercpoj.tmpmgmt.rendering.application;

import java.time.ZoneId;
import java.util.Locale;

/** Resolved locale/timezone/currency triple used by {@link VariableFormatter}. */
public record FormatterSettings(Locale locale, ZoneId zone, String currency) {}
