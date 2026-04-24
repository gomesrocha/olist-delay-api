package dev.fabiorocha.api;

import jakarta.validation.constraints.NotBlank;

public record DelayPredictionRequest(
        @NotBlank
        String cidade,

        @NotBlank
        String estado,

        Integer mesCompra,

        Integer diaSemanaCompra,

        Integer diasPrometidos
) {
}