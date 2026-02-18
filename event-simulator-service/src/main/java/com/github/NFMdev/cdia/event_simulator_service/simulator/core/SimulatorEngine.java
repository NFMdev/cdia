package com.github.NFMdev.cdia.event_simulator_service.simulator.core;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.NFMdev.cdia.common.dto.EventDto;
import com.github.NFMdev.cdia.event_simulator_service.simulator.config.SimulatorProperties;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SimulatorEngine {

    private final SimulatorProperties props;
    private final WebClient webClient;
    private final EventFactory factory;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong sequence = new AtomicLong(0);
    private final AtomicInteger inflight = new AtomicInteger(0);

    private final Counter sentOk;
    private final Counter sentError;
    private final Timer httpLatency;

    private volatile Disposable loop;

    public SimulatorEngine(SimulatorProperties props, WebClient.Builder builder, MeterRegistry registry) {
        this.props = props;
        this.webClient = builder.build();
        this.factory = new EventFactory(props);

        this.sentOk = Counter.builder("simulator_sent_total").tag("status", "ok").register(registry);
        this.sentError = Counter.builder("simulator_sent_total").tag("status", "error").register(registry);
        this.httpLatency = Timer.builder("simulator_http_latency").register(registry);

        registry.gauge("simulator_inflight", inflight);
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true))
            return;

        int eps = Math.max(1, props.getEventsPerSecond());
        long periodMs = Math.max(1L, 1000L / eps);

        loop = Flux.interval(Duration.ZERO, Duration.ofMillis(periodMs))
                .onBackpressureDrop()
                .takeWhile(t -> running.get())
                .flatMap(tick -> emitForTick(tick), props.getConcurrency())
                .subscribe();

    }

    public synchronized void stop() {
        running.set(false);
        if (loop != null)
            loop.dispose();
    }

    public boolean isRunning() {
        return running.get();
    }

    private Mono<Void> emitForTick(long tick) {
        boolean burst = isBurstNow(tick);
        int multiplier = burst ? Math.max(1, props.getBurst().getMultiplier()) : 1;

        // emit N events for this tick if in burst mode
        return Flux.range(0, multiplier)
                .flatMap(i -> sendOne(tick), Math.min(multiplier, props.getConcurrency()))
                .then();
    }

    private boolean isBurstNow(long tick) {
        if (!props.getBurst().isEnabled())
            return false;

        // Approx: ticks per second = eps => convert tick to seconds
        long sec = tick / Math.max(1, props.getEventsPerSecond());
        long every = Math.max(1, props.getBurst().getEverySeconds());
        long dur = Math.max(1, props.getBurst().getDurationSeconds());

        long mod = sec % every;
        return mod < dur;
    }

    private Mono<Void> sendOne(long tick) {
        long seq = sequence.incrementAndGet();
        var rng = factory.forTick(seq);

        boolean burst = isBurstNow(tick);
        EventDto event = factory.create(seq, burst, rng);

        inflight.incrementAndGet();
        Timer.Sample sample = Timer.start();

        return webClient.post()
                .uri(props.getTargetUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(2))
                .doOnSuccess(r -> sentOk.increment())
                .doOnError(e -> sentError.increment())
                .doFinally(sig -> {
                    inflight.decrementAndGet();
                    sample.stop(httpLatency);
                })
                .then();
    }
}
