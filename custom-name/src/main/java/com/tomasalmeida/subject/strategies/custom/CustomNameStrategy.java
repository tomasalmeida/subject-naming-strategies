package com.tomasalmeida.subject.strategies.custom;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy;

import java.util.Map;

public class CustomNameStrategy implements SubjectNameStrategy {
    @Override
    public boolean usesSchema() {
        return true;
    }

    @Override
    public String subjectName(String topic, boolean isKey, ParsedSchema schema) {
        if (topic == null || schema == null) {
            return null;
        }
        String schemaName = schema.name();
        if (schemaName == null) {
            return null;
        }
        String prefix = isKey ? "key-" : "value-";
        return prefix + topic + "-" + schemaName;
    }

    @Override
    public void configure(Map<String, ?> config) {
    }
}
