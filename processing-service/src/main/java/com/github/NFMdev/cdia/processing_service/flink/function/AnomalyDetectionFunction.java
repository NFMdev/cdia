package com.github.NFMdev.cdia.processing_service.flink.function;

import com.github.NFMdev.cdia.processing_service.flink.model.Event;
import com.github.NFMdev.cdia.processing_service.flink.model.EventAnomaly;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class AnomalyDetectionFunction extends KeyedProcessFunction<String, Event, EventAnomaly> {

    private static final String ALERT_STATE_OPEN = "OPEN";
    private static final String ALERT_STATE_ONGOING = "ONGOING";

    private final long openThreshold;
    private final long closeThreshold;
    private final long windowSizeMillis;
    private final long reAlertIntervalMillis;
    private final String ruleDescription;

    private transient MapState<Long, Long> expirationsByTimestamp;
    private transient ValueState<Long> activeEventsCount;
    private transient ValueState<Boolean> alertOpen;
    private transient ValueState<Long> latestEventTimestamp;
    private transient ValueState<Long> nextReAlertProcessingTimer;

    public AnomalyDetectionFunction(
            long openThreshold,
            long closeThreshold,
            Duration windowSize,
            Duration reAlertInterval
    ) {
        if (openThreshold <= closeThreshold) {
            throw new IllegalArgumentException("openThreshold must be greater than closeThreshold for hysteresis.");
        }
        if (reAlertInterval.isZero() || reAlertInterval.isNegative()) {
            throw new IllegalArgumentException("reAlertInterval must be positive.");
        }
        this.openThreshold = openThreshold;
        this.closeThreshold = closeThreshold;
        this.windowSizeMillis = windowSize.toMillis();
        this.reAlertIntervalMillis = reAlertInterval.toMillis();
        this.ruleDescription = ">" + openThreshold + " events in 1 min (close <= " + closeThreshold + ")";
    }

    @Override
    public void open(OpenContext context) {
        expirationsByTimestamp = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("expirations-by-ts", Long.class, Long.class)
        );
        activeEventsCount = getRuntimeContext().getState(new ValueStateDescriptor<>("active-events-count", Long.class));
        alertOpen = getRuntimeContext().getState(new ValueStateDescriptor<>("alert-open", Boolean.class));
        latestEventTimestamp = getRuntimeContext().getState(new ValueStateDescriptor<>("latest-event-ts", Long.class));
        nextReAlertProcessingTimer = getRuntimeContext().getState(
                new ValueStateDescriptor<>("next-realert-processing-timer", Long.class)
        );
    }

    @Override
    public void processElement(Event event, Context context, Collector<EventAnomaly> out) throws Exception {
        if (event.getCreatedAt() == null) {
            return;
        }

        long eventTs = event.getCreatedAt().getTime();
        latestEventTimestamp.update(eventTs);

        long expirationTs = eventTs + windowSizeMillis;
        Long currentlyExpiringAtTs = expirationsByTimestamp.get(expirationTs);
        long updatedExpiringAtTs = (currentlyExpiringAtTs == null ? 0L : currentlyExpiringAtTs) + 1L;
        expirationsByTimestamp.put(expirationTs, updatedExpiringAtTs);
        context.timerService().registerEventTimeTimer(expirationTs);

        long currentCount = (activeEventsCount.value() == null ? 0L : activeEventsCount.value()) + 1L;
        activeEventsCount.update(currentCount);

        boolean isOpen = Boolean.TRUE.equals(alertOpen.value());
        if (!isOpen && currentCount > openThreshold) {
            alertOpen.update(true);
            emitAnomaly(out, context.getCurrentKey(), currentCount, eventTs, ALERT_STATE_OPEN);
            scheduleNextReAlert(context);
            return;
        }

        if (isOpen && nextReAlertProcessingTimer.value() == null) {
            scheduleNextReAlert(context);
        }
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext context, Collector<EventAnomaly> out) throws Exception {
        if (context.timeDomain() == TimeDomain.EVENT_TIME) {
            handleEventTimeTimer(timestamp, context);
            return;
        }

        if (context.timeDomain() == TimeDomain.PROCESSING_TIME) {
            handleReAlertTimer(timestamp, context, out);
        }
    }

    private void handleEventTimeTimer(long timestamp, OnTimerContext context) throws Exception {
        Long expiringEvents = expirationsByTimestamp.get(timestamp);
        if (expiringEvents == null) {
            return;
        }

        expirationsByTimestamp.remove(timestamp);

        long currentCount = (activeEventsCount.value() == null ? 0L : activeEventsCount.value()) - expiringEvents;
        if (currentCount <= 0L) {
            activeEventsCount.clear();
            closeAlert(context);
            return;
        }

        activeEventsCount.update(currentCount);
        if (currentCount <= closeThreshold) {
            closeAlert(context);
        }
    }

    private void handleReAlertTimer(long timestamp, OnTimerContext context, Collector<EventAnomaly> out) throws Exception {
        Long expectedTimerTs = nextReAlertProcessingTimer.value();
        if (expectedTimerTs == null || expectedTimerTs != timestamp) {
            return;
        }
        nextReAlertProcessingTimer.clear();

        if (!Boolean.TRUE.equals(alertOpen.value())) {
            return;
        }

        long currentCount = activeEventsCount.value() == null ? 0L : activeEventsCount.value();
        if (currentCount <= closeThreshold) {
            closeAlert(context);
            return;
        }

        Long lastEventTs = latestEventTimestamp.value();
        if (lastEventTs == null || (timestamp - lastEventTs) > windowSizeMillis) {
            closeAlert(context);
            return;
        }

        emitAnomaly(out, context.getCurrentKey(), currentCount, lastEventTs, ALERT_STATE_ONGOING);
        scheduleNextReAlert(context);
    }

    private void scheduleNextReAlert(Context context) throws Exception {
        Long previousTimerTs = nextReAlertProcessingTimer.value();
        if (previousTimerTs != null) {
            context.timerService().deleteProcessingTimeTimer(previousTimerTs);
        }
        long nextTimerTs = context.timerService().currentProcessingTime() + reAlertIntervalMillis;
        context.timerService().registerProcessingTimeTimer(nextTimerTs);
        nextReAlertProcessingTimer.update(nextTimerTs);
    }

    private void closeAlert(OnTimerContext context) throws Exception {
        alertOpen.clear();

        Long nextTimerTs = nextReAlertProcessingTimer.value();
        if (nextTimerTs != null) {
            context.timerService().deleteProcessingTimeTimer(nextTimerTs);
            nextReAlertProcessingTimer.clear();
        }
    }

    private void emitAnomaly(
            Collector<EventAnomaly> out,
            String location,
            long eventCount,
            long windowEndTs,
            String alertState
    ) {
        String description = ALERT_STATE_OPEN.equals(alertState)
                ? "More than " + openThreshold + " events detected in " + location + " within 1 minute."
                : "Alert still active in " + location + " (" + eventCount + " events in last minute).";

        out.collect(new EventAnomaly(
                UUID.randomUUID(),
                location,
                eventCount,
                new Timestamp(windowEndTs - windowSizeMillis),
                new Timestamp(windowEndTs),
                Timestamp.from(Instant.now()),
                ruleDescription,
                "HIGH",
                alertState,
                description
        ));
    }
}
