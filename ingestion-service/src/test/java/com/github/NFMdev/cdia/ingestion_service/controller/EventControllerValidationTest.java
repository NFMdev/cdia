package com.github.NFMdev.cdia.ingestion_service.controller;

import com.github.NFMdev.cdia.ingestion_service.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    void createEventReturns400WhenCreatedAtMissing() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"INCIDENT",
                                  "description":"Suspicious activity",
                                  "location":"Main Street",
                                  "sourceSystem":"SIMULATOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.createdAt").exists());
    }

    @Test
    void createEventReturns400WhenLocationBlank() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"INCIDENT",
                                  "description":"Suspicious activity",
                                  "location":"   ",
                                  "createdAt":"2026-03-06T12:00:00",
                                  "sourceSystem":"SIMULATOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.location").exists());
    }

    @Test
    void createEventReturns400WhenTypeBlank() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"   ",
                                  "description":"Suspicious activity",
                                  "location":"Main Street",
                                  "createdAt":"2026-03-06T12:00:00",
                                  "sourceSystem":"SIMULATOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.type").exists());
    }

    @Test
    void createEventReturns400WhenSourceSystemBlank() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"INCIDENT",
                                  "description":"Suspicious activity",
                                  "location":"Main Street",
                                  "createdAt":"2026-03-06T12:00:00",
                                  "sourceSystem":"   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.sourceSystem").exists());
    }
}
