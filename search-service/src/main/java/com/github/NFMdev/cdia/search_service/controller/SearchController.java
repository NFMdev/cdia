package com.github.NFMdev.cdia.search_service.controller;

import com.github.NFMdev.cdia.common.contract.SearchEventDocument;
import com.github.NFMdev.cdia.search_service.model.AnomalyPageResponse;
import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;
import com.github.NFMdev.cdia.search_service.model.EventDocument;
import com.github.NFMdev.cdia.search_service.service.AnomalyStreamService;
import com.github.NFMdev.cdia.search_service.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final AnomalyStreamService anomalyStreamService;

    @PostMapping("/internal/index/events")
    public EventDocument indexInternal(@RequestBody SearchEventDocument payload) {
        return searchService.indexInternal(payload);
    }

    @PostMapping("/index")
    public EventDocument indexLegacy(@RequestBody SearchEventDocument payload) {
        return searchService.indexInternal(payload);
    }

    @GetMapping("/description/{text}")
    public List<EventDocument> searchByDescription(@PathVariable String text) {
        return searchService.searchEventByDescription(text);
    }

    @GetMapping("/location/{location}")
    public List<EventDocument> searchByLocation(@PathVariable String location) {
        return searchService.searchEventByLocation(location);
    }

    @GetMapping("/source-system/{name}")
    public List<EventDocument> searchBySourceSystem(@PathVariable String name) {
        return searchService.searchEventBySourceSystem(name);
    }

    @GetMapping("/events")
    public List<EventDocument> findAllEvents() {
        return searchService.findAllEvents();
    }

    @DeleteMapping("/events")
    public void deleteAllEvents() {
        searchService.deleteAllEvents();
    }

    @GetMapping("/anomalies")
    public AnomalyPageResponse findAll(
            @RequestParam(required = false) List<String> severity,
            @RequestParam(required = false) List<String> type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "true") boolean onlyActive) {
        return searchService.findAllAnomalies(
                severity == null ? List.of() : severity,
                type == null ? List.of() : type,
                from,
                to,
                page,
                size,
                sort,
                onlyActive
        );
    }

    @GetMapping("/anomalies/{id}")
    public ResponseEntity<EventAnomalyDocument> findById(@PathVariable("id") String anomalyId) {
        return searchService.findAnomalyById(anomalyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/anomalies")
    public void deleteAllAnomalies() {
        searchService.deleteAllAnomalies();
    }

    @GetMapping(path = "/anomalies/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnomalies(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            @RequestParam(value = "since", required = false) String since) {
        return anomalyStreamService.subscribe(lastEventId, since);
    }
}
