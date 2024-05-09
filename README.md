# Subject name strategies

## Start the environment and code compilation

```shell
    cd env
    docker-compose up -d
    cd ..
    # compile the project and move to the app folder
    mvn clean package
```

Control center is available under http://localhost:9021

## DEMO 1: Record Name Strategy

Creating the needed topics and compiling the project

```shell
  # create topic
  cd env/
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.users --config confluent.value.subject.name.strategy=io.confluent.kafka.serializers.subject.RecordNameStrategy
  cd ..
  cd record-name
```

### Run producer 

Check events being created or refused due to the condition rules.

```shell
   java -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -classpath target/record-name-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.subject.strategies.record.ProducerRunner 
```

### Run consumer

Check events being consumed and transformed during consumption.

```shell
   java -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -classpath target/record-name-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.subject.strategies.record.ConsumerRunner
```


## DEMO 2.a: Custom Name Strategy

Creating the needed topics and compiling the project

```shell
  # create topic
  cd env/
  docker-compose exec broker1 kafka-topics --bootstrap-server broker1:9092 --create --topic crm.users2 --config confluent.value.subject.name.strategy=com.tomasalmeida.subject.strategies.custom.CustomNameStrategy
  cd ..
  cd custom-name
```

### Run producer

Check events being created or refused due to the condition rules.

```shell
   java -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -classpath target/custom-name-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.subject.strategies.custom.ProducerRunner 
```

### Checking the schema

```shell
  # check the schema
   curl -s -X GET http://schema-registry:8081/subjects | jq
[
  "value-crm.users2-com.tomasalmeida.subject.strategies.User"
]
```

Result should contain `value-crm.users2-com.tomasalmeida.subject.strategies.User` in the list.

### Run consumer

Check events being consumed and transformed during consumption.

```shell
   java -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -classpath target/custom-name-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.subject.strategies.custom.ConsumerRunner
```

## DEMO 2.b: Using subject alias

Once finished running the part A of this demo, let's create a subject alias to use the topic name strategy for the topic crm.users2

```shell
    curl -X PUT -H "Content-Type: application/json" http://schema-registry:8081/config/crm.users2-value \
    --data '{"alias": "value-crm.users2-com.tomasalmeida.subject.strategies.User"}' 
```

### Checking the subjects:
    
```shell
  curl -s -X GET http://schema-registry:8081/subjects | jq
[
  "value-crm.users2-com.tomasalmeida.subject.strategies.User"
]
```

Only the custom subject `value-crm.users2-com.tomasalmeida.subject.strategies.User` is visible. Although, the alias can be accessed using.

```shell
   curl -s -X GET http://schema-registry:8081/subjects/crm.users2-value/versions/1 | jq
{
  "subject": "value-crm.users2-com.tomasalmeida.subject.strategies.User",
  "version": 1,
  "id": 1,
  "schema": "{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"com.tomasalmeida.subject.strategies\",\"fields\":[{\"name\":\"firstName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"lastName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"fullName\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"\"},{\"name\":\"age\",\"type\":\"int\"}]}"
}
```


### Run producer

Check events being created or refused due to the condition rules.

```shell
   java -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -classpath target/alias-subject-name-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.subject.strategies.alias.ProducerRunner 
```

### Checking the schema

```shell
  # check the schema
  curl -X GET http://schema-registry:8081/subjects
```

Result should contain `value-crm.users2-com.tomasalmeida.subject.strategies.User` in the list.

### Run consumer

Check events being consumed and transformed during consumption.

```shell
   java -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -classpath target/alias-subject-name-1.0.0-SNAPSHOT-jar-with-dependencies.jar com.tomasalmeida.subject.strategies.alias.ConsumerRunner
```

Findings:
- Data is consumed using topic name strategy
- Data is produced using the custom or topic name strategy

You can confirm the schema is the same using

```shell
  kafka-avro-console-consumer --topic crm.users2 \
  --from-beginning \
  --bootstrap-server localhost:29092 \
  --property schema.registry.url=http://localhost:8081 \
  --property print.schema.ids=true
...
{"firstName":"Tomas","lastName":"Dias Almeida","fullName":"Tomas Almeida","age":39}     1
{"firstName":"Fernando","lastName":"Perez Machado","fullName":"","age":53}      1
{"firstName":"Tomas","lastName":"Dias Almeida","fullName":"Tomas Almeida","age":39}     1
{"firstName":"Fernando","lastName":"Perez Machado","fullName":"","age":53}      1
```

## Shutdown

1. Stop the consumers and producers
2. Stop the environment

```shell
    cd env
    docker-compose down -v
    cd ..
```


## DEMO 3: Using subject alias on Cloud

Export Cloud and Schema Registry API keys and secret. Also, export the schema registry url and id.

```shell
export CLOUD_KEY="xxxx"
export CLOUD_SECRET="yyyyy"

export SR_KEY="oooo"
export SR_SECRET="zzzzz"

export CC_SR_HOST="psrc-abcde.us-east-1.XYZ.confluent.cloud"
export CC_SR_ID="lsrc-fghiz"
```

Create a simple schema

```shell
curl -s -u $SR_KEY:$SR_SECRET POST -H "Content-Type: application/vnd.schemaregistry.v1+json" https://$CC_SR_HOST/subjects/kafka-value/versions --data '{"schema": "{\"type\": \"string\"}", "schemaType": "AVRO" }'
```


Count the schemas (adapt the intervals)

```shell
curl -s 'https://api.telemetry.confluent.cloud/v2/metrics/cloud/query' \
 -H "Content-Type: application/json" \
 -d '{"aggregations":   [{"metric":"io.confluent.kafka.schema_registry/schema_count"}],"filter":{"op":"OR","filters":[{"field":"resource.schema_registry.id","op":"EQ","value":"'$CC_SR_ID'"}]},"granularity":"PT5M","intervals":["2024-05-09T11:00:00+02:00/2024-05-09T12:00:00+02:00"],"limit":1000}' \
 -u $CLOUD_KEY:$CLOUD_SECRET | jq
```

> [!NOTE]  
> As expected only 1 schema is found (the one created above).

Create some aliases:

```shell
curl -X PUT -H "Content-Type: application/json" https://$CC_SR_HOST/config/alias.kafka-value -u $SR_KEY:$SR_SECRET \
 --data '{"alias": "kafka-value"}'

curl -X PUT -H "Content-Type: application/json" https://$CC_SR_HOST/config/alias2.kafka-value -u $SR_KEY:$SR_SECRET \
 --data '{"alias": "kafka-value"}'
 
curl -X PUT -H "Content-Type: application/json" https://$CC_SR_HOST/config/alias3.kafka-value -u $SR_KEY:$SR_SECRET \
 --data '{"alias": "kafka-value"}'
```

Create an alias of an alias

```
curl -X PUT -H "Content-Type: application/json" https://$CC_SR_HOST/config/alias.alias.kafka-value -u $SR_KEY:$SR_SECRET \
 --data '{"alias": "alias.kafka-value"}'
```

> [!WARNING]  
> Alias of alias can be created, but not used. As this could derive on infinite loop or exhaustive usage of CPU.


Count the schemas

```shell
curl -s 'https://api.telemetry.confluent.cloud/v2/metrics/cloud/query' \
 -H "Content-Type: application/json" \
 -d '{"aggregations":   [{"metric":"io.confluent.kafka.schema_registry/schema_count"}],"filter":{"op":"OR","filters":[{"field":"resource.schema_registry.id","op":"EQ","value":"'$CC_SR_ID'"}]},"granularity":"PT5M","intervals":["2024-05-09T11:00:00+02:00/2024-05-09T12:00:00+02:00"],"limit":1000}' \
 -u $CLOUD_KEY:$CLOUD_SECRET | jq
```

Check on the schema itself and its alias

```shell
curl -s -u $SR_KEY:$SR_SECRET GET https://$CC_SR_HOST/subjects/kafka-value/versions/1 | jq

curl -s -u $SR_KEY:$SR_SECRET GET https://$CC_SR_HOST/subjects/alias.kafka-value/versions/1 | jq

curl -s -u $SR_KEY:$SR_SECRET GET https://$CC_SR_HOST/subjects/alias.alias.kafka-value/versions/1 | jq
```

> [!IMPORTANT]  
> As described above, alis of alias returns an error.


Create another schema
```shell
curl -s -u $SR_KEY:$SR_SECRET POST -H "Content-Type: application/vnd.schemaregistry.v1+json" https://$CC_SR_HOST/subjects/kafka2-value/versions --data '{"schema": "{\"type\": \"string\"}", "schemaType": "AVRO" }'
```


Count again the schemas

```
curl -s 'https://api.telemetry.confluent.cloud/v2/metrics/cloud/query' \
 -H "Content-Type: application/json" \
 -d '{"aggregations":   [{"metric":"io.confluent.kafka.schema_registry/schema_count", "agg": "SUM"}],"filter":{"op":"OR","filters":[{"field":"resource.schema_registry.id","op":"EQ","value":"'$CC_SR_ID'"}]},"granularity":"PT5M","intervals":["2024-05-09T11:00:00+02:00/2024-05-09T12:00:00+02:00"],"limit":1000}' \
 -u $CLOUD_KEY:$CLOUD_SECRET | jq
```

Second schema is counted (remember to adapt the intervals).

### Lessons learnt
* A subject can have several aliases
* Alias of alias cannot be used, but can be created.
* An alias does not count as part of the used schemas.


References:
- Docs and Blogs: 
  - https://docs.confluent.io/platform/7.6/schema-registry/schema-validation.html#sr-per-topic-subject-name-strategy
  - https://docs.confluent.io/platform/current/schema-registry/fundamentals/index.html#subject-aliases
  - https://www.confluent.io/blog/best-practices-for-confluent-schema-registry/
