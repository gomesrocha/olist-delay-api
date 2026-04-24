package dev.fabiorocha.ml;
import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalizeCity(String value) {
        if (value == null || value.isBlank()) {
            return "__OTHER__";
        }

        String normalized = Normalizer.normalize(
                value.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ");

        return normalized.trim();
    }

    public static String normalizeState(String value) {
        if (value == null || value.isBlank()) {
            return "NA";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }
}