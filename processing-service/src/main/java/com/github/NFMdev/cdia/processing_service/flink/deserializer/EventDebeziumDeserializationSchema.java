package com.github.NFMdev.cdia.processing_service.flink.deserializer;

import com.github.NFMdev.cdia.processing_service.flink.model.Event;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.cdc.connectors.shaded.org.apache.kafka.connect.data.Struct;
import org.apache.flink.cdc.connectors.shaded.org.apache.kafka.connect.source.SourceRecord;
import org.apache.flink.cdc.debezium.DebeziumDeserializationSchema;
import org.apache.flink.util.Collector;

import java.sql.Timestamp;

public class EventDebeziumDeserializationSchema implements DebeziumDeserializationSchema<Event> {

    @Override
    public void deserialize(SourceRecord sourceRecord, Collector<Event> collector) {
        Struct valueStruct = (Struct) sourceRecord.value();
        if (valueStruct == null) {
            return;
        }

        String operation = valueStruct.getString("op");
        if (!"c".equals(operation) && !"r".equals(operation) && !"u".equals(operation)) {
            return;
        }

        Struct after = (Struct) valueStruct.get("after");
        if (after != null) {
            Object createdAtRaw = after.get("created_at");
            Timestamp createdAt = parseDebeziumTimestamp(createdAtRaw);
            if (createdAt == null) {
                return;
            }
            Long eventId = parseDebeziumLong(after.get("id"));
            if (eventId == null) {
                return;
            }

            Event event = new Event(
                    eventId,
                    getOptionalString(after, "type"),
                    after.getString("description"),
                    after.getString("location"),
                    parseDebeziumLong(after.get("source_id")),
                    createdAt
            );
            collector.collect(event);
        }
    }

    @Override
    public TypeInformation<Event> getProducedType() {
        return TypeInformation.of(Event.class);
    }

    private Timestamp parseDebeziumTimestamp(Object rawTimestamp) {
        if (rawTimestamp == null) {
            return null;
        }

        if (rawTimestamp instanceof java.util.Date dateValue) {
            return new Timestamp(dateValue.getTime());
        }

        if (!(rawTimestamp instanceof Number numericValue)) {
            throw new IllegalArgumentException("Unsupported Debezium timestamp type: " + rawTimestamp.getClass());
        }

        long raw = numericValue.longValue();
        long absolute = Math.abs(raw);

        if (absolute >= 1_000_000_000_000_000L) {
            return new Timestamp(raw / 1_000L);
        }

        if (absolute >= 1_000_000_000_000L) {
            return new Timestamp(raw);
        }

        return new Timestamp(raw * 1_000L);
    }

    private Long parseDebeziumLong(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number numericValue) {
            return numericValue.longValue();
        }
        if (rawValue instanceof String stringValue) {
            return Long.parseLong(stringValue);
        }
        throw new IllegalArgumentException("Unsupported Debezium numeric type: " + rawValue.getClass());
    }

    private String getOptionalString(Struct struct, String fieldName) {
        if (struct == null || struct.schema() == null || struct.schema().field(fieldName) == null) {
            return null;
        }
        return struct.getString(fieldName);
    }
}
