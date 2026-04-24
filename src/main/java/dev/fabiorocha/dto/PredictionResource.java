package dev.fabiorocha.dto;


import dev.fabiorocha.ml.DelayPredictionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/predicoes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(
        name = "Predição de Atraso",
        description = "Endpoints para estimar o risco de atraso de pedidos usando modelo treinado com dados da Olist."
)
public class PredictionResource {

    @Inject
    DelayPredictionService predictionService;

    @POST
    @Path("/atraso")
    @Operation(
            summary = "Prediz risco de atraso de entrega",
            description = """
                    Recebe dados básicos do pedido, como cidade, estado, mês da compra,
                    dia da semana e quantidade de dias prometidos, e retorna a probabilidade
                    estimada de atraso com base no modelo treinado em Weka.
                    """
    )
    public DelayPredictionResponse predictDelay(@Valid DelayPredictionRequest request) {
        return predictionService.predict(request);
    }
}