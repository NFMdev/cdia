package com.github.NFMdev.cdia.ingestion_service.client.search;

import com.github.NFMdev.cdia.common.contract.SearchEventDocument;
import com.github.NFMdev.cdia.common.dto.EventDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class SearchIndexingService {

    private static final String INDEXING_URI = "/search/internal/index/events";

    private final RestClient restClient;
    private final boolean indexingEnabled;
    private final int maxAttempts;
    private final long retryBackoffMillis;
    private final Counter deadLetterCounter;

    public SearchIndexingService(
            RestClient.Builder restClientBuilder,
            @Value("${cdia.search.base-url:http://search-service:8085}") String searchBaseUrl,
            @Value("${cdia.search.indexing.enabled:true}") boolean indexingEnabled,
            @Value("${cdia.search.indexing.max-attempts:3}") int maxAttempts,
            @Value("${cdia.search.indexing.retry-backoff-ms:200}") long retryBackoffMillis,
            MeterRegistry meterRegistry) {
        this.restClient = restClientBuilder
                .baseUrl(searchBaseUrl)
                .build();
        this.indexingEnabled = indexingEnabled;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoffMillis = Math.max(0L, retryBackoffMillis);
        this.deadLetterCounter = Counter.builder("cdia.search.indexing.dead_letter.total")
                .description("Number of events that failed indexing after retries")
                .register(meterRegistry);
    }

    public void index(EventDto event) {
        if (!indexingEnabled || event == null) {
            return;
        }

        SearchEventDocument document = SearchEventDocument.fromEvent(event);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri(INDEXING_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(document)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    deadLetterCounter.increment();
                    log.error(
                            "Dead-lettered event {} after {} indexing attempts: {}",
                            event.getId(),
                            maxAttempts,
                            ex.getMessage()
                    );
                    return;
                }
                log.warn(
                        "Indexing attempt {}/{} failed for event {}: {}",
                        attempt,
                        maxAttempts,
                        event.getId(),
                        ex.getMessage()
                );
                sleepBackoff();
            }
        }
    }

    private void sleepBackoff() {
        if (retryBackoffMillis <= 0L) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
