package com.github.NFMdev.cdia.search_service.controller;

import com.github.NFMdev.cdia.search_service.service.AnomalyStreamService;
import com.github.NFMdev.cdia.search_service.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerStreamEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private AnomalyStreamService anomalyStreamService;

    @Test
    void streamEndpointReturnsSseEmitterAndForwardsResumeInputs() throws Exception {
        when(anomalyStreamService.subscribe(eq("123"), isNull())).thenReturn(new SseEmitter(0L));

        mockMvc.perform(get("/search/anomalies/stream")
                        .header("Last-Event-ID", "123"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(anomalyStreamService).subscribe(eq("123"), isNull());
    }
}
