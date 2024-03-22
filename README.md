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
  curl -X GET http://schema-registry:8081/subjects
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
  curl -X GET http://schema-registry:8081/subjects
```

Only the custom subject `value-crm.users2-com.tomasalmeida.subject.strategies.User` is visible. Although, the alias can be accessed using.

```shell
   curl -s -X GET http://schema-registry:8081/subjects/crm.users2-value/versions/1 | jq
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
```

## Shutdown

1. Stop the consumers and producers
2. Stop the environment

```shell
    cd env
    docker-compose down -v
    cd ..
```

References:
- Docs and Blogs: 
  - https://docs.confluent.io/platform/7.6/schema-registry/schema-validation.html#sr-per-topic-subject-name-strategy
  - https://docs.confluent.io/platform/current/schema-registry/fundamentals/index.html#subject-aliases
  - https://www.confluent.io/blog/best-practices-for-confluent-schema-registry/
  