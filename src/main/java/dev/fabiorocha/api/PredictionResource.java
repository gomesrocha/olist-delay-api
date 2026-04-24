package dev.fabiorocha.api;
import dev.fabiorocha.ml.DelayPredictionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;

@Path("/api/v1/predicoes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PredictionResource {

    @Inject
    DelayPredictionService predictionService;

    @POST
    @Path("/atraso")
    public DelayPredictionResponse predictDelay(@Valid DelayPredictionRequest request) {
        return predictionService.predict(request);
    }
}