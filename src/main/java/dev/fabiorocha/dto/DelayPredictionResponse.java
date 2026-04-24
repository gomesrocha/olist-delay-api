package dev.fabiorocha.dto;

import dev.fabiorocha.ml.RiskLevel;

public record DelayPredictionResponse(
        boolean possivelAtraso,
        double probabilidadeAtraso,
        RiskLevel risco,
        String mensagem
) {
}