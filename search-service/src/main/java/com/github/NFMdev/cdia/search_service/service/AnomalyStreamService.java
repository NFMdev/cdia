package com.github.NFMdev.cdia.search_service.service;

import com.github.NFMdev.cdia.search_service.model.AnomalyPageResponse;
import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;
import com.github.NFMdev.cdia.search_service.util.AnomalyTimestampNormalizer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyStreamService {

    private static final long EMITTER_TIMEOUT_MILLIS = 120_000L;
    private static final long POLL_INTERVAL_MILLIS = 3_000L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 15_000L;
    private static final int STREAM_BUFFER_SIZE = 2_000;
    private static final int POLL_BATCH_SIZE = 200;
    private static final int CATCH_UP_BATCH_SIZE = 200;

    private final SearchService searchService;

    private final ConcurrentHashMap<String, ClientSession> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> fingerprintByAnomalyId = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<Long, StreamEvent> eventBuffer = new ConcurrentSkipListMap<>();
    private final AtomicLong sequence = new AtomicLong(0L);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "anomaly-sse-poller");
        thread.setDaemon(true);
        return thread;
    });

    @PostConstruct
    public void startPolling() {
        scheduler.scheduleWithFixedDelay(this::pollAndPublishChanges, 0L, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(this::emitHeartbeats, HEARTBEAT_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    public SseEmitter subscribe(String lastEventId, String since) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        String clientId = UUID.randomUUID().toString();
        long resumeEventId = parseResumeEventId(lastEventId);
        ClientSession session = new ClientSession(clientId, emitter, new AtomicLong(resumeEventId));
        clients.put(clientId, session);

        emitter.onCompletion(() -> removeClient(clientId));
        emitter.onTimeout(() -> {
            removeClient(clientId);
            emitter.complete();
        });
        emitter.onError(error -> removeClient(clientId));

        replayFromBuffer(session, resumeEventId);
        if (isOutsideBuffer(resumeEventId) || resumeEventId <= 0L) {
            emitCatchUpSnapshot(session, since);
        }

        return emitter;
    }

    private void pollAndPublishChanges() {
        try {
            List<EventAnomalyDocument> anomalies = searchService.findRecentAnomalies(POLL_BATCH_SIZE).stream()
                    .sorted(Comparator
                            .comparing(
                                    (EventAnomalyDocument item) -> AnomalyTimestampNormalizer.parseInstant(item.getDetectedAt()),
                                    Comparator.nullsLast(Comparator.naturalOrder())
                            )
                            .thenComparing(EventAnomalyDocument::getId, Comparator.nullsLast(String::compareTo)))
                    .toList();

            Set<String> currentIds = new HashSet<>();
            for (EventAnomalyDocument anomaly : anomalies) {
                if (anomaly.getId() == null) {
                    continue;
                }

                currentIds.add(anomaly.getId());
                String fingerprint = fingerprint(anomaly);
                String previousFingerprint = fingerprintByAnomalyId.put(anomaly.getId(), fingerprint);
                if (fingerprint.equals(previousFingerprint)) {
                    continue;
                }

                publish(anomaly);
            }

            fingerprintByAnomalyId.keySet().retainAll(currentIds);
        } catch (Exception ex) {
            log.warn("Failed to poll anomalies for SSE stream", ex);
        }
    }

    private void publish(EventAnomalyDocument anomaly) {
        long eventId = sequence.incrementAndGet();
        StreamEvent streamEvent = new StreamEvent(eventId, anomaly);
        eventBuffer.put(eventId, streamEvent);
        trimBuffer();

        clients.forEach((clientId, session) -> sendEvent(session, streamEvent, false));
    }

    private void replayFromBuffer(ClientSession session, long resumeEventId) {
        if (eventBuffer.isEmpty()) {
            return;
        }

        eventBuffer.tailMap(resumeEventId + 1L).values()
                .forEach(streamEvent -> sendEvent(session, streamEvent, true));
    }

    private boolean isOutsideBuffer(long resumeEventId) {
        if (resumeEventId <= 0L || eventBuffer.isEmpty()) {
            return false;
        }
        Long oldest = eventBuffer.firstKey();
        return oldest != null && resumeEventId < oldest;
    }

    private void emitCatchUpSnapshot(ClientSession session, String since) {
        List<EventAnomalyDocument> snapshot;
        Instant sinceInstant = AnomalyTimestampNormalizer.parseInstant(since);
        if (sinceInstant != null) {
            AnomalyPageResponse response = searchService.findAllAnomalies(
                    List.of(),
                    List.of(),
                    sinceInstant.toString(),
                    null,
                    0,
                    CATCH_UP_BATCH_SIZE,
                    "detectedAt:asc",
                    false
            );
            snapshot = response.items();
        } else {
            snapshot = searchService.findRecentAnomalies(CATCH_UP_BATCH_SIZE).stream()
                    .sorted(Comparator
                            .comparing(
                                    (EventAnomalyDocument item) -> AnomalyTimestampNormalizer.parseInstant(item.getDetectedAt()),
                                    Comparator.nullsLast(Comparator.naturalOrder())
                            )
                            .thenComparing(EventAnomalyDocument::getId, Comparator.nullsLast(String::compareTo)))
                    .toList();
        }

        for (EventAnomalyDocument anomaly : snapshot) {
            long snapshotEventId = sequence.incrementAndGet();
            sendEvent(session, new StreamEvent(snapshotEventId, anomaly), true);
        }
    }

    private void emitHeartbeats() {
        clients.forEach((clientId, session) -> {
            synchronized (session) {
                try {
                    session.emitter().send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException ex) {
                    removeClient(clientId);
                }
            }
        });
    }

    private void sendEvent(ClientSession session, StreamEvent event, boolean isReplay) {
        synchronized (session) {
            try {
                session.emitter().send(SseEmitter.event()
                        .name(isReplay ? "anomaly-replay" : "anomaly")
                        .id(Long.toString(event.eventId()))
                        .data(event.anomaly()));
                session.lastSentEventId().set(event.eventId());
            } catch (IOException ex) {
                removeClient(session.id());
                session.emitter().complete();
                log.debug("Closed anomaly SSE client {} due to send failure", session.id(), ex);
            }
        }
    }

    private long parseResumeEventId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(headerValue.trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void trimBuffer() {
        while (eventBuffer.size() > STREAM_BUFFER_SIZE) {
            eventBuffer.pollFirstEntry();
        }
    }

    private String fingerprint(EventAnomalyDocument anomaly) {
        return safe(anomaly.getLocation())
                + "|" + anomaly.getEventCount()
                + "|" + safe(anomaly.getWindowStart())
                + "|" + safe(anomaly.getWindowEnd())
                + "|" + safe(anomaly.getDetectedAt())
                + "|" + safe(anomaly.getRule())
                + "|" + safe(anomaly.getSeverity())
                + "|" + safe(anomaly.getType())
                + "|" + safe(anomaly.getAlertState())
                + "|" + safe(anomaly.getDescription());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void removeClient(String clientId) {
        clients.remove(clientId);
    }

    private record StreamEvent(long eventId, EventAnomalyDocument anomaly) {
    }

    private record ClientSession(String id, SseEmitter emitter, AtomicLong lastSentEventId) {
    }
}
