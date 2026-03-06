package com.github.NFMdev.cdia.search_service.util;

import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public final class AnomalyTimestampNormalizer {

    private AnomalyTimestampNormalizer() {
    }

    public static EventAnomalyDocument normalize(EventAnomalyDocument source) {
        if (source == null) {
            return null;
        }

        return new EventAnomalyDocument(
                source.getId(),
                source.getLocation(),
                source.getEventCount(),
                toIsoOrNull(source.getWindowStart()),
                toIsoOrNull(source.getWindowEnd()),
                toIsoOrNull(source.getDetectedAt()),
                source.getRule(),
                source.getSeverity(),
                source.getType(),
                source.getAlertState(),
                source.getDescription());
    }

    public static String toIsoOrNull(String value) {
        Instant instant = parseInstant(value);
        return instant == null ? null : instant.toString();
    }

    public static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.matches("\\d+")) {
            try {
                long numeric = Long.parseLong(trimmed);
                return trimmed.length() <= 10 ? Instant.ofEpochSecond(numeric) : Instant.ofEpochMilli(numeric);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Continue with local date-time fallback.
        }

        String normalized = trimmed.replace(' ', 'T');
        try {
            return LocalDateTime.parse(normalized).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
