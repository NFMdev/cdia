package com.github.NFMdev.cdia.processing_service.flink.function;

import com.github.NFMdev.cdia.processing_service.flink.model.Event;
import com.github.NFMdev.cdia.processing_service.flink.model.EventAnomaly;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

public class AnomalyDetectionFunction extends KeyedProcessFunction<String, Event, EventAnomaly> {

    private static final String ALERT_STATE_OPEN = "OPEN";
    private static final String ALERT_STATE_ONGOING = "ONGOING";
    private static final String ALERT_STATE_CLOSED = "CLOSED";
    private static final String SEVERITY_HIGH = "HIGH";

    private final long openThreshold;
    private final long closeThreshold;
    private final long windowSizeMillis;
    private final long reAlertIntervalMillis;
    private final String ruleDescription;

    private transient MapState<Long, Long> expirationsByTimestamp;
    private transient MapState<Long, Long> firstEventIdByExpirationTimestamp;
    private transient MapState<Long, Long> lastEventIdByExpirationTimestamp;
    private transient ValueState<Long> activeEventsCount;
    private transient ValueState<Boolean> alertOpen;
    private transient ValueState<Long> earliestActiveExpirationTimestamp;
    private transient ValueState<Long> latestActiveExpirationTimestamp;
    private transient ValueState<Long> nextReAlertProcessingTimer;
    private transient ValueState<String> activeAnomalyId;

    public AnomalyDetectionFunction(
            long openThreshold,
            long closeThreshold,
            Duration windowSize,
            Duration reAlertInterval) {
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
                new MapStateDescriptor<>("expirations-by-ts", Long.class, Long.class));
        firstEventIdByExpirationTimestamp = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("first-event-id-by-expiration-ts", Long.class, Long.class));
        lastEventIdByExpirationTimestamp = getRuntimeContext().getMapState(
                new MapStateDescriptor<>("last-event-id-by-expiration-ts", Long.class, Long.class));
        activeEventsCount = getRuntimeContext().getState(new ValueStateDescriptor<>("active-events-count", Long.class));
        alertOpen = getRuntimeContext().getState(new ValueStateDescriptor<>("alert-open", Boolean.class));
        earliestActiveExpirationTimestamp = getRuntimeContext().getState(
                new ValueStateDescriptor<>("earliest-active-expiration-ts", Long.class));
        latestActiveExpirationTimestamp = getRuntimeContext().getState(
                new ValueStateDescriptor<>("latest-active-expiration-ts", Long.class));
        nextReAlertProcessingTimer = getRuntimeContext().getState(
                new ValueStateDescriptor<>("next-realert-processing-timer", Long.class));
        activeAnomalyId = getRuntimeContext().getState(
                new ValueStateDescriptor<>("anomaly-id", String.class));
    }

    @Override
    public void processElement(Event event, Context context, Collector<EventAnomaly> out) throws Exception {
        if (event.getCreatedAt() == null) {
            return;
        }

        long eventTs = event.getCreatedAt().getTime();
        long expirationTs = eventTs + windowSizeMillis;
        incrementExpiringCount(expirationTs);
        updateEventWindowBounds(expirationTs, event.getId());
        context.timerService().registerEventTimeTimer(expirationTs);

        long currentCount = updateActiveCountBy(1L);
        WindowSnapshot windowSnapshot = resolveWindowSnapshot(eventTs);

        boolean isOpen = isAlertOpen();
        if (!isOpen && currentCount > openThreshold) {
            alertOpen.update(true);
            String anomalyId = ensureAnomalyId(context.getCurrentKey(), eventTs);
            emitAnomaly(
                    out,
                    anomalyId,
                    context.getCurrentKey(),
                    currentCount,
                    windowSnapshot.windowEndTs(),
                    ALERT_STATE_OPEN,
                    windowSnapshot.firstEventId(),
                    windowSnapshot.lastEventId());
            scheduleNextReAlert(context);
            return;
        }

        if (isOpen && nextReAlertProcessingTimer.value() == null) {
            scheduleNextReAlert(context);
        }
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext context, Collector<EventAnomaly> out) throws Exception {
        switch (context.timeDomain()) {
            case EVENT_TIME -> handleEventTimeTimer(timestamp, context, out);
            case PROCESSING_TIME -> handleReAlertTimer(timestamp, context, out);
            default -> {
            }
        }
    }

    private void handleEventTimeTimer(long timestamp, OnTimerContext context, Collector<EventAnomaly> out)
            throws Exception {
        Long expiringEvents = expirationsByTimestamp.get(timestamp);
        if (expiringEvents == null) {
            return;
        }

        expirationsByTimestamp.remove(timestamp);
        firstEventIdByExpirationTimestamp.remove(timestamp);
        lastEventIdByExpirationTimestamp.remove(timestamp);
        updateBoundaryExpirationPointers(timestamp);

        long currentCount = updateActiveCountBy(-expiringEvents);
        WindowSnapshot windowSnapshot = resolveWindowSnapshot(timestamp);
        if (currentCount <= 0L) {
            if (isAlertOpen()) {
                closeAlert(
                        context,
                        out,
                        0L,
                        windowSnapshot.windowEndTs(),
                        windowSnapshot.firstEventId(),
                        windowSnapshot.lastEventId());
            } else {
                clearAnomaly();
            }
            clearActiveWindowState();
            return;
        }

        if (currentCount <= closeThreshold) {
            if (isAlertOpen()) {
                closeAlert(
                        context,
                        out,
                        currentCount,
                        windowSnapshot.windowEndTs(),
                        windowSnapshot.firstEventId(),
                        windowSnapshot.lastEventId());
            } else {
                clearAnomaly();
            }
        }
    }

    private void handleReAlertTimer(long timestamp, OnTimerContext context, Collector<EventAnomaly> out)
            throws Exception {
        Long expectedTimerTs = nextReAlertProcessingTimer.value();
        if (expectedTimerTs == null || expectedTimerTs != timestamp) {
            return;
        }
        nextReAlertProcessingTimer.clear();

        if (!isAlertOpen()) {
            return;
        }

        long currentCount = currentActiveCount();
        WindowSnapshot windowSnapshot = resolveWindowSnapshot(timestamp);
        if (currentCount <= closeThreshold) {
            closeAlert(
                    context,
                    out,
                    currentCount,
                    windowSnapshot.windowEndTs(),
                    windowSnapshot.firstEventId(),
                    windowSnapshot.lastEventId());
            return;
        }

        Long lastEventTs = latestActiveEventTimestamp();
        if (lastEventTs == null || (timestamp - lastEventTs) > windowSizeMillis) {
            closeAlert(
                    context,
                    out,
                    currentCount,
                    windowSnapshot.windowEndTs(),
                    windowSnapshot.firstEventId(),
                    windowSnapshot.lastEventId());
            return;
        }

        String anomalyId = ensureAnomalyId(context.getCurrentKey(), lastEventTs);

        emitAnomaly(
                out,
                anomalyId,
                context.getCurrentKey(),
                currentCount,
                lastEventTs,
                ALERT_STATE_ONGOING,
                windowSnapshot.firstEventId(),
                windowSnapshot.lastEventId());
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

    private void closeAlert(
            OnTimerContext context,
            Collector<EventAnomaly> out,
            long eventCount,
            long windowEndTs,
            Long firstEventId,
            Long lastEventId) throws Exception {
        String anomalyId = activeAnomalyId.value();
        if (anomalyId != null && isAlertOpen()) {
            emitAnomaly(
                    out,
                    anomalyId,
                    context.getCurrentKey(),
                    eventCount,
                    windowEndTs,
                    ALERT_STATE_CLOSED,
                    firstEventId,
                    lastEventId);
        }

        alertOpen.clear();

        Long nextTimerTs = nextReAlertProcessingTimer.value();
        if (nextTimerTs != null) {
            context.timerService().deleteProcessingTimeTimer(nextTimerTs);
            nextReAlertProcessingTimer.clear();
        }

        clearAnomaly();
    }

    private void emitAnomaly(
            Collector<EventAnomaly> out,
            String anomalyId,
            String location,
            long eventCount,
            long windowEndTs,
            String alertState,
            Long firstEventId,
            Long lastEventId) {
        String description = buildDescription(alertState, location, eventCount);

        out.collect(new EventAnomaly(
                anomalyId,
                location,
                eventCount,
                new Timestamp(windowEndTs - windowSizeMillis),
                new Timestamp(windowEndTs),
                Timestamp.from(Instant.now()),
                ruleDescription,
                SEVERITY_HIGH,
                alertState,
                description,
                firstEventId,
                lastEventId));
    }

    private void updateEventWindowBounds(long expirationTs, Long eventId) throws Exception {
        Long earliestExpirationTs = earliestActiveExpirationTimestamp.value();
        if (earliestExpirationTs == null || expirationTs < earliestExpirationTs) {
            earliestActiveExpirationTimestamp.update(expirationTs);
        }

        Long latestExpirationTs = latestActiveExpirationTimestamp.value();
        if (latestExpirationTs == null || expirationTs > latestExpirationTs) {
            latestActiveExpirationTimestamp.update(expirationTs);
        }

        if (eventId == null) {
            return;
        }

        Long firstEventId = firstEventIdByExpirationTimestamp.get(expirationTs);
        if (firstEventId == null || eventId < firstEventId) {
            firstEventIdByExpirationTimestamp.put(expirationTs, eventId);
        }

        Long lastEventId = lastEventIdByExpirationTimestamp.get(expirationTs);
        if (lastEventId == null || eventId > lastEventId) {
            lastEventIdByExpirationTimestamp.put(expirationTs, eventId);
        }
    }

    private void updateBoundaryExpirationPointers(long removedExpirationTs) throws Exception {
        Long earliestExpirationTs = earliestActiveExpirationTimestamp.value();
        Long latestExpirationTs = latestActiveExpirationTimestamp.value();
        if ((earliestExpirationTs != null && earliestExpirationTs.equals(removedExpirationTs))
                || (latestExpirationTs != null && latestExpirationTs.equals(removedExpirationTs))) {
            recomputeBoundaryExpirations();
        }
    }

    private void recomputeBoundaryExpirations() throws Exception {
        Long earliestExpirationTs = null;
        Long latestExpirationTs = null;
        for (Long expirationTs : expirationsByTimestamp.keys()) {
            if (earliestExpirationTs == null || expirationTs < earliestExpirationTs) {
                earliestExpirationTs = expirationTs;
            }
            if (latestExpirationTs == null || expirationTs > latestExpirationTs) {
                latestExpirationTs = expirationTs;
            }
        }

        if (earliestExpirationTs == null) {
            earliestActiveExpirationTimestamp.clear();
            latestActiveExpirationTimestamp.clear();
            return;
        }

        earliestActiveExpirationTimestamp.update(earliestExpirationTs);
        latestActiveExpirationTimestamp.update(latestExpirationTs);
    }

    private void clearActiveWindowState() throws Exception {
        activeEventsCount.clear();
        expirationsByTimestamp.clear();
        firstEventIdByExpirationTimestamp.clear();
        lastEventIdByExpirationTimestamp.clear();
        earliestActiveExpirationTimestamp.clear();
        latestActiveExpirationTimestamp.clear();
    }

    private Long latestActiveEventTimestamp() throws Exception {
        Long latestExpirationTs = latestActiveExpirationTimestamp.value();
        if (latestExpirationTs == null) {
            return null;
        }
        return latestExpirationTs - windowSizeMillis;
    }

    private long resolveWindowEndTs(long fallbackWindowEndTs) throws Exception {
        Long latestWindowEndTs = latestActiveEventTimestamp();
        return latestWindowEndTs == null ? fallbackWindowEndTs : latestWindowEndTs;
    }

    private Long resolveFirstEventId() throws Exception {
        Long earliestExpirationTs = earliestActiveExpirationTimestamp.value();
        if (earliestExpirationTs == null) {
            return null;
        }
        return firstEventIdByExpirationTimestamp.get(earliestExpirationTs);
    }

    private Long resolveLastEventId() throws Exception {
        Long latestExpirationTs = latestActiveExpirationTimestamp.value();
        if (latestExpirationTs == null) {
            return null;
        }
        return lastEventIdByExpirationTimestamp.get(latestExpirationTs);
    }

    private WindowSnapshot resolveWindowSnapshot(long fallbackWindowEndTs) throws Exception {
        return new WindowSnapshot(
                resolveWindowEndTs(fallbackWindowEndTs),
                resolveFirstEventId(),
                resolveLastEventId());
    }

    private String ensureAnomalyId(String location, long openEventTs) throws Exception {
        String current = activeAnomalyId.value();
        if (current != null) {
            return current;
        }

        String id = "LOC_SPIKE:" + location + ":" + openEventTs;
        activeAnomalyId.update(id);
        return id;
    }

    private void clearAnomaly() throws Exception {
        activeAnomalyId.clear();
    }

    private long currentActiveCount() throws Exception {
        Long value = activeEventsCount.value();
        return value == null ? 0L : value;
    }

    private long updateActiveCountBy(long delta) throws Exception {
        long updated = Math.max(0L, currentActiveCount() + delta);
        if (updated == 0L) {
            activeEventsCount.clear();
        } else {
            activeEventsCount.update(updated);
        }
        return updated;
    }

    private boolean isAlertOpen() throws Exception {
        return Boolean.TRUE.equals(alertOpen.value());
    }

    private void incrementExpiringCount(long expirationTs) throws Exception {
        Long current = expirationsByTimestamp.get(expirationTs);
        expirationsByTimestamp.put(expirationTs, (current == null ? 0L : current) + 1L);
    }

    private String buildDescription(String alertState, String location, long eventCount) {
        return switch (alertState) {
            case ALERT_STATE_OPEN -> "More than " + openThreshold + " events detected in "
                    + location + " within 1 minute.";
            case ALERT_STATE_CLOSED -> "Alert closed in " + location
                    + " (" + eventCount + " events in last minute).";
            default -> "Alert still active in " + location + " (" + eventCount + " events in last minute).";
        };
    }

    private record WindowSnapshot(long windowEndTs, Long firstEventId, Long lastEventId) {
    }
}
