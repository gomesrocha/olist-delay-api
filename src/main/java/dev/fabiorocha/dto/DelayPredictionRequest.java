package dev.fabiorocha.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(
        name = "DelayPredictionRequest",
        description = "Dados necessários para estimar o risco de atraso de entrega de um pedido."
)
public record DelayPredictionRequest(

        @NotBlank(message = "A cidade é obrigatória.")
        @Schema(
                description = "Cidade do cliente ou cidade de destino do pedido. Deve ser informada preferencialmente sem abreviações.",
                example = "sao paulo",
                required = true
        )
        String cidade,

        @NotBlank(message = "O estado é obrigatório.")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "O estado deve ser informado como UF com 2 letras. Exemplo: SP, RJ, PE.")
        @Schema(
                description = "Unidade Federativa do cliente ou destino do pedido, informada com a sigla de 2 letras.",
                example = "SP",
                required = true
        )
        String estado,

        @Min(value = 1, message = "O mês da compra deve ser entre 1 e 12.")
        @Max(value = 12, message = "O mês da compra deve ser entre 1 e 12.")
        @Schema(
                description = "Mês em que a compra foi realizada. Use valores de 1 a 12, onde 1 representa janeiro e 12 representa dezembro.",
                example = "4",
                required = false
        )
        Integer mesCompra,

        @Min(value = 1, message = "O dia da semana deve ser entre 1 e 7.")
        @Max(value = 7, message = "O dia da semana deve ser entre 1 e 7.")
        @Schema(
                description = "Dia da semana em que a compra foi realizada. Use 1 para segunda-feira, 2 para terça-feira, até 7 para domingo.",
                example = "2",
                required = false
        )
        Integer diaSemanaCompra,

        @PositiveOrZero(message = "A quantidade de dias prometidos não pode ser negativa.")
        @Schema(
                description = "Quantidade de dias entre a data da compra e a data estimada de entrega prometida ao cliente.",
                example = "8",
                required = false
        )
        Integer diasPrometidos

) {
}