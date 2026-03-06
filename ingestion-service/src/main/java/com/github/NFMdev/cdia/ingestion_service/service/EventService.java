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
        normalizeEvent(eventDto);
        EventEntity entity = eventMapper.toEntity(eventDto);
        EventEntity saved = eventRepository.save(entity);
        EventDto savedDto = eventMapper.toDto(saved);
        publisher.publishEvent(new EventCreatedEvent(this, savedDto));
        return savedDto;
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

    private void normalizeEvent(EventDto eventDto) {
        if (eventDto.getStatus() == null || eventDto.getStatus().isBlank()) {
            eventDto.setStatus("INGESTED");
        }
        if (eventDto.getType() == null || eventDto.getType().isBlank()) {
            eventDto.setType("INCIDENT");
        }
        if (eventDto.getLocation() != null) {
            eventDto.setLocation(eventDto.getLocation().trim());
        }
        if (eventDto.getDescription() != null) {
            eventDto.setDescription(eventDto.getDescription().trim());
        }
        if (eventDto.getSourceSystem() != null) {
            eventDto.setSourceSystem(eventDto.getSourceSystem().trim());
        }
    }
}
