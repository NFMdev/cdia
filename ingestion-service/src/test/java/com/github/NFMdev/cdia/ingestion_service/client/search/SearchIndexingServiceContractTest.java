package com.github.NFMdev.cdia.ingestion_service.client.search;

import com.github.NFMdev.cdia.common.dto.EventDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SearchIndexingServiceContractTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void indexingUsesVersionedInternalContractAndRetriesBeforeSuccess() {
        mockServer.expect(requestTo("http://search-service:8085/search/internal/index/events"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());
        mockServer.expect(requestTo("http://search-service:8085/search/internal/index/events"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());
        mockServer.expect(requestTo("http://search-service:8085/search/internal/index/events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"id\":\"7\"")))
                .andRespond(withSuccess());

        SearchIndexingService service = new SearchIndexingService(
                restClientBuilder,
                "http://search-service:8085",
                true,
                3,
                0L,
                meterRegistry
        );

        service.index(buildEvent());

        mockServer.verify();
    }

    @Test
    void indexingIncrementsDeadLetterMetricAfterExhaustingRetries() {
        mockServer.expect(requestTo("http://search-service:8085/search/internal/index/events"))
                .andRespond(withServerError());
        mockServer.expect(requestTo("http://search-service:8085/search/internal/index/events"))
                .andRespond(withServerError());
        mockServer.expect(requestTo("http://search-service:8085/search/internal/index/events"))
                .andRespond(withServerError());

        SearchIndexingService service = new SearchIndexingService(
                restClientBuilder,
                "http://search-service:8085",
                true,
                3,
                0L,
                meterRegistry
        );

        service.index(buildEvent());

        mockServer.verify();
        assertEquals(
                1.0d,
                meterRegistry.get("cdia.search.indexing.dead_letter.total").counter().count()
        );
    }

    private EventDto buildEvent() {
        EventDto event = new EventDto();
        event.setId(7L);
        event.setType("INCIDENT");
        event.setDescription("Suspicious activity");
        event.setLocation("Aalborg");
        event.setStatus("INGESTED");
        event.setCreatedAt(LocalDateTime.parse("2026-03-06T12:00:00"));
        event.setSourceSystem("SIMULATOR");
        return event;
    }
}
