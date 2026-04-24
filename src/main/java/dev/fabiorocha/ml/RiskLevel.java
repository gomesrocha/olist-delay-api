package dev.fabiorocha.ml;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum RiskLevel {

    ALTO(0.75),
    MEDIO(0.50),
    BAIXO(0.30),
    MUITO_BAIXO(0.00);

    private final double minProbability;

    RiskLevel(double minProbability) {
        this.minProbability = minProbability;
    }

    public static RiskLevel from(double probability) {
        if (Double.isNaN(probability) || probability < 0 || probability > 1) {
            throw new IllegalArgumentException("Probabilidade inválida: " + probability);
        }

        return Arrays.stream(values())
                .filter(level -> probability >= level.minProbability)
                .findFirst()
                .orElse(MUITO_BAIXO);
    }

    @JsonValue
    public String value() {
        return name();
    }
}