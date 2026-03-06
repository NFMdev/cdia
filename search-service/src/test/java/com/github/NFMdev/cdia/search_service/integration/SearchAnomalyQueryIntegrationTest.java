package com.github.NFMdev.cdia.search_service.integration;

import com.github.NFMdev.cdia.search_service.model.AnomalyPageResponse;
import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;
import com.github.NFMdev.cdia.search_service.repository.EventAnomalyRepository;
import com.github.NFMdev.cdia.search_service.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SearchAnomalyQueryIntegrationTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:8.13.2"
    ).withEnv("xpack.security.enabled", "false");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
        registry.add("spring.elasticsearch.username", () -> "");
        registry.add("spring.elasticsearch.password", () -> "");
    }

    @Autowired
    private EventAnomalyRepository eventAnomalyRepository;

    @Autowired
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        eventAnomalyRepository.deleteAll();
    }

    @Test
    void anomalyQueryAppliesFiltersAndPagingInElasticsearch() {
        eventAnomalyRepository.save(new EventAnomalyDocument(
                "a-1",
                "Aalborg",
                20L,
                "2026-03-06T11:59:00Z",
                "2026-03-06T12:00:00Z",
                "2026-03-06T12:00:00Z",
                "rule",
                "HIGH",
                "ALERT",
                "OPEN",
                "first"
        ));
        eventAnomalyRepository.save(new EventAnomalyDocument(
                "a-2",
                "Aalborg",
                5L,
                "2026-03-06T11:49:00Z",
                "2026-03-06T11:50:00Z",
                "2026-03-06T11:50:00Z",
                "rule",
                "LOW",
                "INCIDENT",
                "CLOSED",
                "second"
        ));
        eventAnomalyRepository.save(new EventAnomalyDocument(
                "a-3",
                "Aalborg",
                32L,
                "2026-03-06T12:04:00Z",
                "2026-03-06T12:05:00Z",
                "2026-03-06T12:05:00Z",
                "rule",
                "HIGH",
                "ALERT",
                "ONGOING",
                "third"
        ));

        awaitAtMost(Duration.ofSeconds(5), () ->
                searchService.findAllAnomalies(
                        List.of("HIGH"),
                        List.of("ALERT"),
                        "2026-03-06T11:30:00Z",
                        null,
                        0,
                        1,
                        "detectedAt:desc",
                        true
                ).total() >= 2
        );

        AnomalyPageResponse page = searchService.findAllAnomalies(
                List.of("HIGH"),
                List.of("ALERT"),
                "2026-03-06T11:30:00Z",
                null,
                0,
                1,
                "detectedAt:desc",
                true
        );

        assertEquals(1, page.items().size());
        assertEquals("a-3", page.items().getFirst().getId());
        assertEquals(2L, page.total());
        assertTrue(page.hasNext());
    }

    private void awaitAtMost(Duration timeout, Condition condition) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.isSatisfied()) {
                return;
            }
            sleep(200L);
        }
        throw new AssertionError("Condition was not satisfied within " + timeout);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface Condition {
        boolean isSatisfied();
    }
}
