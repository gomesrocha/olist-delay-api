package dev.fabiorocha.ml;

import dev.fabiorocha.api.DelayPredictionRequest;
import dev.fabiorocha.api.DelayPredictionResponse;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.InputStream;

@ApplicationScoped
public class DelayPredictionService {

    @ConfigProperty(name = "app.model.file", defaultValue = "model/olist-delay-randomforest.model")
    String modelFile;

    @ConfigProperty(name = "app.model.header", defaultValue = "model/olist-delay-header.model")
    String headerFile;

    @ConfigProperty(name = "app.prediction.threshold", defaultValue = "0.50")
    double threshold;

    private Classifier classifier;
    private Instances header;

    @PostConstruct
    void init() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            try (InputStream modelStream = cl.getResourceAsStream(modelFile);
                 InputStream headerStream = cl.getResourceAsStream(headerFile)) {

                if (modelStream == null) {
                    throw new IllegalStateException("Modelo não encontrado: " + modelFile);
                }

                if (headerStream == null) {
                    throw new IllegalStateException("Header não encontrado: " + headerFile);
                }

                this.classifier = (Classifier) SerializationHelper.read(modelStream);
                this.header = (Instances) SerializationHelper.read(headerStream);
                this.header.setClassIndex(this.header.numAttributes() - 1);
            }

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao carregar modelo Weka", e);
        }
    }

    public DelayPredictionResponse predict(DelayPredictionRequest request) {
        try {
            Instance instance = new DenseInstance(header.numAttributes());
            instance.setDataset(header);

            setNominalValue(instance, "cidade", TextNormalizer.normalizeCity(request.cidade()), "__OTHER__");
            setNominalValue(instance, "estado", TextNormalizer.normalizeState(request.estado()), "NA");

            setNumericOrMissing(instance, "mes_compra", request.mesCompra());
            setNumericOrMissing(instance, "dia_semana_compra", request.diaSemanaCompra());
            setNumericOrMissing(instance, "dias_prometidos", request.diasPrometidos());

            double[] distribution = classifier.distributionForInstance(instance);

            int atrasadoIndex = header.classAttribute().indexOfValue("sim");
            double probability = distribution[atrasadoIndex];

            boolean possibleDelay = probability >= threshold;

            String risk = classifyRisk(probability);

            String message = possibleDelay
                    ? "Pedido com risco de atraso acima do limite configurado."
                    : "Pedido com baixo risco estimado de atraso.";

            return new DelayPredictionResponse(
                    possibleDelay,
                    round(probability),
                    risk,
                    message
            );

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao realizar predição de atraso", e);
        }
    }

    private void setNominalValue(Instance instance, String attributeName, String value, String fallback) {
        Attribute attribute = header.attribute(attributeName);

        if (attribute == null) {
            throw new IllegalArgumentException("Atributo não encontrado no modelo: " + attributeName);
        }

        String finalValue = attribute.indexOfValue(value) >= 0 ? value : fallback;

        if (attribute.indexOfValue(finalValue) < 0) {
            instance.setMissing(attribute);
            return;
        }

        instance.setValue(attribute, finalValue);
    }

    private void setNumericOrMissing(Instance instance, String attributeName, Integer value) {
        Attribute attribute = header.attribute(attributeName);

        if (attribute == null) {
            throw new IllegalArgumentException("Atributo não encontrado no modelo: " + attributeName);
        }

        if (value == null) {
            instance.setMissing(attribute);
        } else {
            instance.setValue(attribute, value);
        }
    }

    private String classifyRisk(double probability) {
        if (probability >= 0.75) {
            return "ALTO";
        }

        if (probability >= 0.50) {
            return "MEDIO";
        }

        if (probability >= 0.30) {
            return "BAIXO";
        }

        return "MUITO_BAIXO";
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}