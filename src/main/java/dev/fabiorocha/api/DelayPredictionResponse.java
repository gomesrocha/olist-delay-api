package dev.fabiorocha.api;

public record DelayPredictionResponse(
        boolean possivelAtraso,
        double probabilidadeAtraso,
        String risco,
        String mensagem
) {
}