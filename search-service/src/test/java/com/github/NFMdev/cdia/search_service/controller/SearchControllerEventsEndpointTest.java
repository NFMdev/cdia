package com.github.NFMdev.cdia.search_service.controller;

import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;
import com.github.NFMdev.cdia.search_service.service.AnomalyStreamService;
import com.github.NFMdev.cdia.search_service.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerEventsEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private AnomalyStreamService anomalyStreamService;

    @Test
    void deleteEventsEndpointDelegatesToEventDeletion() throws Exception {
        mockMvc.perform(delete("/search/events"))
                .andExpect(status().isOk());

        verify(searchService).deleteAllEvents();
    }

    @Test
    void anomalyByIdEndpointReturns200WhenPresent() throws Exception {
        when(searchService.findAnomalyById(eq("anom-1")))
                .thenReturn(Optional.of(new EventAnomalyDocument()));

        mockMvc.perform(get("/search/anomalies/anom-1"))
                .andExpect(status().isOk());
    }
}
