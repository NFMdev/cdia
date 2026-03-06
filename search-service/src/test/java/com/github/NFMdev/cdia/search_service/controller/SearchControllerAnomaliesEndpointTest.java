package com.github.NFMdev.cdia.search_service.controller;

import com.github.NFMdev.cdia.search_service.model.AnomalyPageResponse;
import com.github.NFMdev.cdia.search_service.service.AnomalyStreamService;
import com.github.NFMdev.cdia.search_service.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerAnomaliesEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private AnomalyStreamService anomalyStreamService;

    @Test
    void anomaliesEndpointBindsRequestParamsWithoutReflectionParameterNames() throws Exception {
        when(searchService.findAllAnomalies(
                List.of("HIGH"),
                List.of("ALERT"),
                "2026-03-06T12:00:00Z",
                "2026-03-06T14:00:00Z",
                2,
                25,
                "eventCount:asc",
                true
        )).thenReturn(new AnomalyPageResponse(List.of(), 2, 25, 0, false));

        mockMvc.perform(get("/search/anomalies")
                        .param("severity", "HIGH")
                        .param("type", "ALERT")
                        .param("from", "2026-03-06T12:00:00Z")
                        .param("to", "2026-03-06T14:00:00Z")
                        .param("page", "2")
                        .param("size", "25")
                        .param("sort", "eventCount:asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(2));

        verify(searchService).findAllAnomalies(
                eq(List.of("HIGH")),
                eq(List.of("ALERT")),
                eq("2026-03-06T12:00:00Z"),
                eq("2026-03-06T14:00:00Z"),
                eq(2),
                eq(25),
                eq("eventCount:asc"),
                eq(true)
        );
    }

    @Test
    void anomaliesEndpointAcceptsOnlyActiveFalse() throws Exception {
        when(searchService.findAllAnomalies(List.of(), List.of(), null, null, 0, 50, null, false))
                .thenReturn(new AnomalyPageResponse(List.of(), 0, 50, 0, false));

        mockMvc.perform(get("/search/anomalies")
                        .param("onlyActive", "false"))
                .andExpect(status().isOk());

        verify(searchService).findAllAnomalies(
                eq(List.of()),
                eq(List.of()),
                isNull(),
                isNull(),
                eq(0),
                eq(50),
                isNull(),
                eq(false));
    }
}
