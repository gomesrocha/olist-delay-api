package dev.fabiorocha.training;

import dev.fabiorocha.ml.TextNormalizer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TrainDelayModel {

    private static final String OTHER_CITY = "__OTHER__";
    private static final String UNKNOWN_STATE = "NA";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso:");
            System.out.println("java TrainDelayModel <pasta-dataset-olist> <pasta-saida-modelo>");
            System.exit(1);
        }

        Path datasetPath = Path.of(args[0]);
        Path outputPath = Path.of(args[1]);

        Files.createDirectories(outputPath);

        Path ordersCsv = datasetPath.resolve("olist_orders_dataset.csv");
        Path customersCsv = datasetPath.resolve("olist_customers_dataset.csv");

        Map<String, CustomerLocation> customers = loadCustomers(customersCsv);
        List<OrderTrainingRow> rows = loadOrders(ordersCsv, customers);

        System.out.println("Total de registros válidos para treino: " + rows.size());

        Set<String> topCities = selectTopCities(rows, 300);
        Instances data = buildDataset(rows, topCities);

        RandomForest randomForest = new RandomForest();
        randomForest.setNumIterations(200);
        randomForest.setSeed(42);
        randomForest.buildClassifier(data);

        Evaluation evaluation = new Evaluation(data);
        evaluation.crossValidateModel(randomForest, data, 10, new Random(42));

        System.out.println(evaluation.toSummaryString());
        System.out.println(evaluation.toClassDetailsString());
        System.out.println(evaluation.toMatrixString());

        Path modelFile = outputPath.resolve("olist-delay-randomforest.model");
        Path headerFile = outputPath.resolve("olist-delay-header.model");

        SerializationHelper.write(modelFile.toString(), randomForest);
        SerializationHelper.write(headerFile.toString(), data.stringFreeStructure());

        System.out.println("Modelo salvo em: " + modelFile);
        System.out.println("Header salvo em: " + headerFile);
    }

    private static Map<String, CustomerLocation> loadCustomers(Path customersCsv) throws IOException {
        Map<String, CustomerLocation> customers = new HashMap<>();

        try (CSVParser parser = CSVParser.parse(
                customersCsv,
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
        )) {
            for (CSVRecord record : parser) {
                String customerId = record.get("customer_id");
                String city = TextNormalizer.normalizeCity(record.get("customer_city"));
                String state = TextNormalizer.normalizeState(record.get("customer_state"));

                customers.put(customerId, new CustomerLocation(city, state));
            }
        }

        return customers;
    }

    private static List<OrderTrainingRow> loadOrders(
            Path ordersCsv,
            Map<String, CustomerLocation> customers
    ) throws IOException {

        List<OrderTrainingRow> rows = new ArrayList<>();

        try (CSVParser parser = CSVParser.parse(
                ordersCsv,
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
        )) {
            for (CSVRecord record : parser) {
                String status = record.get("order_status");

                if (!"delivered".equalsIgnoreCase(status)) {
                    continue;
                }

                String customerId = record.get("customer_id");
                CustomerLocation location = customers.get(customerId);

                if (location == null) {
                    continue;
                }

                String purchaseDateText = record.get("order_purchase_timestamp");
                String deliveredDateText = record.get("order_delivered_customer_date");
                String estimatedDateText = record.get("order_estimated_delivery_date");

                if (isBlank(purchaseDateText) || isBlank(deliveredDateText) || isBlank(estimatedDateText)) {
                    continue;
                }

                LocalDateTime purchaseDate = LocalDateTime.parse(purchaseDateText.replace(" ", "T"));
                LocalDateTime deliveredDate = LocalDateTime.parse(deliveredDateText.replace(" ", "T"));
                LocalDateTime estimatedDate = LocalDateTime.parse(estimatedDateText.replace(" ", "T"));

                boolean delayed = deliveredDate.isAfter(estimatedDate);

                int month = purchaseDate.getMonthValue();
                DayOfWeek dayOfWeek = purchaseDate.getDayOfWeek();
                int promisedDays = Math.max(0, (int) Duration.between(purchaseDate, estimatedDate).toDays());

                rows.add(new OrderTrainingRow(
                        location.city(),
                        location.state(),
                        month,
                        dayOfWeek.getValue(),
                        promisedDays,
                        delayed
                ));
            }
        }

        return rows;
    }

    private static Set<String> selectTopCities(List<OrderTrainingRow> rows, int limit) {
        Map<String, Long> frequency = rows.stream()
                .collect(Collectors.groupingBy(OrderTrainingRow::city, Collectors.counting()));

        return frequency.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Instances buildDataset(List<OrderTrainingRow> rows, Set<String> topCities) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        ArrayList<String> cityValues = new ArrayList<>(topCities);
        cityValues.add(OTHER_CITY);

        ArrayList<String> stateValues = new ArrayList<>(
                rows.stream()
                        .map(OrderTrainingRow::state)
                        .filter(state -> state != null && !state.isBlank())
                        .collect(Collectors.toCollection(TreeSet::new))
        );

        if (!stateValues.contains(UNKNOWN_STATE)) {
            stateValues.add(UNKNOWN_STATE);
        }

        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("nao");
        classValues.add("sim");

        attributes.add(new Attribute("cidade", cityValues));
        attributes.add(new Attribute("estado", stateValues));
        attributes.add(new Attribute("mes_compra"));
        attributes.add(new Attribute("dia_semana_compra"));
        attributes.add(new Attribute("dias_prometidos"));
        attributes.add(new Attribute("atraso", classValues));

        Instances data = new Instances("olist_atraso_pedido", attributes, rows.size());
        data.setClassIndex(data.numAttributes() - 1);

        for (OrderTrainingRow row : rows) {
            DenseInstance instance = new DenseInstance(data.numAttributes());
            instance.setDataset(data);

            String city = topCities.contains(row.city()) ? row.city() : OTHER_CITY;
            String state = stateValues.contains(row.state()) ? row.state() : UNKNOWN_STATE;

            instance.setValue(data.attribute("cidade"), city);
            instance.setValue(data.attribute("estado"), state);
            instance.setValue(data.attribute("mes_compra"), row.month());
            instance.setValue(data.attribute("dia_semana_compra"), row.dayOfWeek());
            instance.setValue(data.attribute("dias_prometidos"), row.promisedDays());
            instance.setValue(data.attribute("atraso"), row.delayed() ? "sim" : "nao");

            data.add(instance);
        }

        return data;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record CustomerLocation(
            String city,
            String state
    ) {
    }

    private record OrderTrainingRow(
            String city,
            String state,
            int month,
            int dayOfWeek,
            int promisedDays,
            boolean delayed
    ) {
    }
}