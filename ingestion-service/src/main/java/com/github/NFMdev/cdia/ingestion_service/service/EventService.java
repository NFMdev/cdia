package com.github.NFMdev.cdia.ingestion_service.service;

import com.github.NFMdev.cdia.common.dto.EventDto;
import com.github.NFMdev.cdia.ingestion_service.event.EventCreatedEvent;
import com.github.NFMdev.cdia.ingestion_service.mapper.EventMapper;
import com.github.NFMdev.cdia.ingestion_service.model.event.EventEntity;
import com.github.NFMdev.cdia.ingestion_service.repository.EventRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final ApplicationEventPublisher publisher;

    public EventService(EventRepository eventRepository, EventMapper eventMapper, ApplicationEventPublisher publisher) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.publisher = publisher;
    }

    @Transactional
    public EventDto saveEvent(EventDto eventDto) {
        EventEntity entity = eventMapper.toEntity(eventDto);
        EventEntity saved = eventRepository.save(entity);
        // Emit event to notify ES of a new event
        publisher.publishEvent(new EventCreatedEvent(this, saved));
        return eventMapper.toDto(saved);
    }

    public EventDto getEventById(Long id) {
        return eventRepository.findById(id)
                .map(eventMapper::toDto)
                .orElse(null);
    }

    public List<EventDto> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }
}
