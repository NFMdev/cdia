package com.github.NFMdev.cdia.ingestion_service.event;

import com.github.NFMdev.cdia.common.dto.EventDto;
import org.springframework.context.ApplicationEvent;

public class EventCreatedEvent extends ApplicationEvent {

    private final EventDto event;

    public EventCreatedEvent(Object source, EventDto event) {
        super(source);
        this.event = event;
    }

    public EventDto getEvent() {
        return event;
    }
}
