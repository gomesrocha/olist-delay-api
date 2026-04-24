# olist-delay-api

API desenvolvida com **Java 21**, **Quarkus** e **Weka** para prever a possibilidade de atraso em pedidos de e-commerce com base em dados históricos do dataset público da **Olist Brazilian E-Commerce**.

O objetivo inicial é treinar um modelo de Machine Learning capaz de classificar se um pedido possui risco de atraso considerando principalmente informações relacionadas à cidade, estado e características temporais do pedido.

---

## 1. Tecnologias utilizadas

- Java 21
- Quarkus 3.x
- Weka
- Maven
- Dataset Olist Brazilian E-Commerce
- REST API com JSON

---

## 2. Dataset utilizado

O projeto utiliza o dataset público:

**Brazilian E-Commerce Public Dataset by Olist**

Fonte:

```text
https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce
```

Arquivos principais utilizados inicialmente:

```text
olist_orders_dataset.csv
olist_customers_dataset.csv
```

O modelo usa os pedidos entregues e cria uma variável alvo indicando se o pedido atrasou ou não.

A regra utilizada é:

```text
atrasado = order_delivered_customer_date > order_estimated_delivery_date
```

---

## 3. Estrutura esperada do projeto

A estrutura básica esperada é:

```text
olist-delay-api/
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── data/
│   └── olist/
│       ├── olist_customers_dataset.csv
│       ├── olist_geolocation_dataset.csv
│       ├── olist_order_items_dataset.csv
│       ├── olist_order_payments_dataset.csv
│       ├── olist_order_reviews_dataset.csv
│       ├── olist_orders_dataset.csv
│       ├── olist_products_dataset.csv
│       └── olist_sellers_dataset.csv
├── src/
│   └── main/
│       ├── java/
│       └── resources/
│           └── model/
│               ├── olist-delay-randomforest.model
│               └── olist-delay-header.model
└── target/
```

---

## 4. Baixando o dataset

Crie a pasta de dados:

```bash
mkdir -p data/olist
```

Baixe o dataset da Olist usando `curl`:

```bash
curl -L -o ~/Downloads/brazilian-ecommerce.zip \
  https://www.kaggle.com/api/v1/datasets/download/olistbr/brazilian-ecommerce
```

Entre na pasta do dataset:

```bash
cd data/olist
```

Descompacte o arquivo baixado:

```bash
unzip -o ~/Downloads/brazilian-ecommerce.zip -d .
```

Volte para a raiz do projeto:

```bash
cd ../..
```

Confirme se os arquivos foram extraídos corretamente:

```bash
ls data/olist
```

Resultado esperado:

```text
olist_customers_dataset.csv
olist_geolocation_dataset.csv
olist_order_items_dataset.csv
olist_order_payments_dataset.csv
olist_order_reviews_dataset.csv
olist_orders_dataset.csv
olist_products_dataset.csv
olist_sellers_dataset.csv
```

---

## 5. Treinando o modelo

O treinamento é executado pela classe:

```text
dev.fabiorocha.training.TrainDelayModel
```

Comando para treinar o modelo:

```bash
mvn compile exec:java \
  -Dexec.mainClass="dev.fabiorocha.training.TrainDelayModel" \
  -Dexec.args="data/olist src/main/resources/model"
```

Ou, usando o Maven Wrapper:

```bash
./mvnw compile exec:java \
  -Dexec.mainClass="dev.fabiorocha.training.TrainDelayModel" \
  -Dexec.args="data/olist src/main/resources/model"
```

O primeiro argumento indica a pasta onde estão os arquivos CSV:

```text
data/olist
```

O segundo argumento indica onde o modelo treinado será salvo:

```text
src/main/resources/model
```

Ao final do treinamento, os seguintes arquivos serão gerados:

```text
src/main/resources/model/olist-delay-randomforest.model
src/main/resources/model/olist-delay-header.model
```

---

## 6. Resultado atual do treinamento

Com a configuração atual, o treinamento utilizou:

```text
Total de registros válidos para treino: 96470
```

Resultado obtido:

```text
Correctly Classified Instances       87157               90.3462 %
Incorrectly Classified Instances      9313                9.6538 %
Kappa statistic                          0.1439
Mean absolute error                      0.1322
Root mean squared error                  0.2828
Total Number of Instances            96470
```

Matriz de confusão:

```text
     a     b   <-- classified as
 86079  2565 |     a = nao
  6748  1078 |     b = sim
```

Interpretação:

- O modelo teve aproximadamente **90,34% de acurácia geral**.
- O modelo classifica bem pedidos que **não atrasam**.
- Porém, a classe `sim`, que representa pedidos atrasados, ainda possui baixo recall.
- Isso indica que o dataset está desbalanceado e o modelo tende a favorecer a classe majoritária `nao`.

Resultado por classe:

```text
Classe nao:
- Recall: 0.971
- Precision: 0.927
- F-Measure: 0.949

Classe sim:
- Recall: 0.138
- Precision: 0.296
- F-Measure: 0.188
```

Portanto, esta versão deve ser tratada como uma **primeira prova de conceito**.

Para melhorar a detecção real de atrasos, recomenda-se evoluir o modelo com:

- balanceamento da base;
- ajuste do threshold de classificação;
- uso de mais atributos;
- uso de informações de vendedor, frete, peso, valor e distância;
- testes com `CostSensitiveClassifier`, `SMOTE` ou outros algoritmos.

---

## 7. Executando a API em modo desenvolvimento

Para subir a aplicação em modo desenvolvimento:

```bash
./mvnw quarkus:dev
```

Ou:

```bash
mvn quarkus:dev
```

A aplicação será iniciada em:

```text
http://localhost:8080
```

O Quarkus Dev UI estará disponível em:

```text
http://localhost:8080/q/dev
```

---

## 8. Testando a API

Endpoint de predição:

```text
POST /api/v1/predicoes/atraso
```

Exemplo de chamada com `curl`:

```bash
curl -X POST http://localhost:8080/api/v1/predicoes/atraso \
  -H "Content-Type: application/json" \
  -d '{
    "cidade": "sao paulo",
    "estado": "SP",
    "mesCompra": 4,
    "diaSemanaCompra": 2,
    "diasPrometidos": 8
  }'
```

Exemplo de resposta esperada:

```json
{
  "possivelAtraso": false,
  "probabilidadeAtraso": 0.1842,
  "risco": "MUITO_BAIXO",
  "mensagem": "Pedido com baixo risco estimado de atraso."
}
```

Outro exemplo:

```bash
curl -X POST http://localhost:8080/api/v1/predicoes/atraso \
  -H "Content-Type: application/json" \
  -d '{
    "cidade": "recife",
    "estado": "PE",
    "mesCompra": 11,
    "diaSemanaCompra": 5,
    "diasPrometidos": 5
  }'
```

---

## 9. Campos da requisição

| Campo | Tipo | Obrigatório | Descrição |
|---|---:|---:|---|
| cidade | string | Sim | Cidade do cliente |
| estado | string | Sim | UF do cliente |
| mesCompra | integer | Não | Mês da compra, de 1 a 12 |
| diaSemanaCompra | integer | Não | Dia da semana, de 1 a 7 |
| diasPrometidos | integer | Não | Diferença em dias entre a data da compra e a data estimada de entrega |

Exemplo:

```json
{
  "cidade": "sao paulo",
  "estado": "SP",
  "mesCompra": 4,
  "diaSemanaCompra": 2,
  "diasPrometidos": 8
}
```

---

## 10. Campos da resposta

| Campo | Tipo | Descrição |
|---|---:|---|
| possivelAtraso | boolean | Indica se o modelo classificou o pedido como possível atraso |
| probabilidadeAtraso | double | Probabilidade estimada para a classe `sim` |
| risco | string | Faixa de risco calculada |
| mensagem | string | Mensagem explicativa da predição |

Exemplo:

```json
{
  "possivelAtraso": true,
  "probabilidadeAtraso": 0.6723,
  "risco": "MEDIO",
  "mensagem": "Pedido com risco de atraso acima do limite configurado."
}
```

---

## 11. Configurações da aplicação

As configurações principais ficam em:

```text
src/main/resources/application.properties
```

Exemplo:

```properties
quarkus.http.port=8080

app.model.file=model/olist-delay-randomforest.model
app.model.header=model/olist-delay-header.model
app.prediction.threshold=0.50
```

O parâmetro abaixo controla o limite mínimo para considerar um pedido como possível atraso:

```properties
app.prediction.threshold=0.50
```

Exemplo:

- `0.50`: classifica como atraso se a probabilidade for maior ou igual a 50%.
- `0.30`: aumenta a sensibilidade para detectar atrasos, mas pode gerar mais falsos positivos.
- `0.70`: reduz falsos positivos, mas pode deixar de identificar atrasos reais.

---

## 12. Empacotando a aplicação

Para gerar o pacote da aplicação:

```bash
./mvnw package
```

O artefato será gerado em:

```text
target/quarkus-app/
```

Execute com:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

---

## 13. Gerando um über-jar

Caso deseje gerar um JAR único:

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

Execute com:

```bash
java -jar target/*-runner.jar
```

---

## 14. Observação sobre Native Image

Esta aplicação utiliza Weka e modelos serializados.

Por isso, recomenda-se executar inicialmente em modo JVM:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

A geração de native image com GraalVM pode exigir configurações adicionais de reflection, resources e serialização.

---

## 15. Próximas melhorias recomendadas

A versão atual usa uma abordagem inicial baseada em cidade, UF e dados temporais.

Para melhorar a qualidade do modelo, recomenda-se adicionar novas variáveis:

```text
freight_value
payment_value
payment_installments
seller_city
seller_state
product_weight_g
product_length_cm
product_height_cm
product_width_cm
distância aproximada entre vendedor e cliente
categoria do produto
histórico de atraso por rota
```

Também é recomendado testar:

```text
RandomForest com ajuste de hiperparâmetros
CostSensitiveClassifier
SMOTE
Logistic Regression
J48
XGBoost externo, se futuramente migrar para outra stack de ML
```

---

## 16. Problemas comuns

### Classe de treinamento não encontrada

Erro:

```text
ClassNotFoundException: TrainDelayModel
```

Verifique se o comando usa o pacote correto:

```bash
-Dexec.mainClass="dev.fabiorocha.training.TrainDelayModel"
```

E se o arquivo existe em:

```text
src/main/java/dev/fabiorocha/training/TrainDelayModel.java
```

---

### Arquivos CSV não encontrados

Verifique se os arquivos estão em:

```text
data/olist
```

Comando para validar:

```bash
ls data/olist
```

---

### Warnings do ARPACK / Netlib

Durante o treinamento, podem aparecer mensagens como:

```text
WARNING: Failed to load implementation from: com.github.fommil.netlib.NativeSystemARPACK
WARNING: Failed to load implementation from: com.github.fommil.netlib.NativeRefARPACK
```

Essas mensagens normalmente não impedem o treinamento. Elas indicam que bibliotecas nativas de álgebra linear não foram carregadas e que será usado fallback em Java.

---

## 17. Status atual

- Dataset baixado com sucesso.
- Dados descompactados em `data/olist`.
- Modelo treinado com 96.470 registros válidos.
- Modelo salvo em `src/main/resources/model`.
- API pronta para execução e teste via REST.

---

## 18. Licença e uso dos dados

Este projeto é uma prova de conceito educacional e experimental.

O dataset pertence à Olist e está disponível publicamente no Kaggle. Consulte os termos de uso diretamente na página do dataset.
