package com.github.NFMdev.cdia.search_service.service;

import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;
import com.github.NFMdev.cdia.search_service.model.EventDocument;
import com.github.NFMdev.cdia.search_service.repository.EventAnomalyRepository;
import com.github.NFMdev.cdia.search_service.repository.EventDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final EventDocumentRepository eventDocumentRepository;
    private final EventAnomalyRepository eventAnomalyRepository;

    public EventDocument index(EventDocument document) {
        return eventDocumentRepository.save(document);
    }

    public List<EventDocument> searchEventByDescription(String description) {
        return eventDocumentRepository.findByDescriptionContainingIgnoreCase(description);
    }

    public List<EventDocument> searchEventByLocation(String location) {
        return eventDocumentRepository.findByLocationContainingIgnoreCase(location);
    }

    public List<EventDocument> searchEventBySourceSystem(String sourceSystem) {
        return eventDocumentRepository.findBySourceSystem(sourceSystem);
    }

    public Iterable<EventDocument> findAllEvents() {
        return eventDocumentRepository.findAll();
    }

    public void deleteAllAnomalies() {
        eventAnomalyRepository.deleteAll();
    }

    public Iterable<EventAnomalyDocument> findAllAnomalies() { return eventAnomalyRepository.findAll(); }
}
