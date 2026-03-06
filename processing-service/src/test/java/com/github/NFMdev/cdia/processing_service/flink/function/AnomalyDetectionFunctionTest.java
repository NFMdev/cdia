package com.github.NFMdev.cdia.processing_service.flink.function;

import com.github.NFMdev.cdia.processing_service.flink.model.Event;
import com.github.NFMdev.cdia.processing_service.flink.model.EventAnomaly;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnomalyDetectionFunctionTest {

    @Test
    void opensAlertWhenThresholdIsExceeded() throws Exception {
        AnomalyDetectionFunction function = new AnomalyDetectionFunction(
                2L,
                1L,
                Duration.ofMinutes(1),
                Duration.ofSeconds(30)
        );

        try (KeyedOneInputStreamOperatorTestHarness<String, Event, EventAnomaly> harness =
                     ProcessFunctionTestHarnesses.forKeyedProcessFunction(function, Event::getLocation, Types.STRING)) {
            harness.open();

            long baseTs = Instant.parse("2026-03-06T12:00:00Z").toEpochMilli();
            harness.processElement(event(1L, baseTs), baseTs);
            harness.processElement(event(2L, baseTs + 1_000L), baseTs + 1_000L);
            harness.processElement(event(3L, baseTs + 2_000L), baseTs + 2_000L);

            List<EventAnomaly> output = outputValues(harness);
            assertEquals(1, output.size());
            assertEquals("OPEN", output.get(0).getAlertState());
            assertEquals(3L, output.get(0).getEventCount());
            assertTrue(output.get(0).getId().contains("Aalborg"));
        }
    }

    @Test
    void closesAlertWhenWindowCountFallsBelowCloseThreshold() throws Exception {
        AnomalyDetectionFunction function = new AnomalyDetectionFunction(
                2L,
                1L,
                Duration.ofMinutes(1),
                Duration.ofSeconds(30)
        );

        try (KeyedOneInputStreamOperatorTestHarness<String, Event, EventAnomaly> harness =
                     ProcessFunctionTestHarnesses.forKeyedProcessFunction(function, Event::getLocation, Types.STRING)) {
            harness.open();

            long baseTs = Instant.parse("2026-03-06T12:00:00Z").toEpochMilli();
            harness.processElement(event(1L, baseTs), baseTs);
            harness.processElement(event(2L, baseTs + 1_000L), baseTs + 1_000L);
            harness.processElement(event(3L, baseTs + 2_000L), baseTs + 2_000L);

            harness.processWatermark(baseTs + Duration.ofMinutes(2).toMillis());

            List<EventAnomaly> output = outputValues(harness);
            assertFalse(output.isEmpty());
            assertEquals("OPEN", output.get(0).getAlertState());
            assertEquals("CLOSED", output.get(output.size() - 1).getAlertState());
        }
    }

    private Event event(Long id, long timestampMillis) {
        return new Event(
                id,
                "INCIDENT",
                "desc",
                "Aalborg",
                1L,
                Timestamp.from(Instant.ofEpochMilli(timestampMillis))
        );
    }

    private List<EventAnomaly> outputValues(KeyedOneInputStreamOperatorTestHarness<String, Event, EventAnomaly> harness) {
        List<EventAnomaly> values = new ArrayList<>();
        for (Object entry : harness.getOutput()) {
            if (entry instanceof StreamRecord<?> streamRecord) {
                values.add((EventAnomaly) streamRecord.getValue());
            }
        }
        return values;
    }
}
