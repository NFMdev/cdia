package com.github.NFMdev.cdia.ingestion_service.listener;

import com.github.NFMdev.cdia.ingestion_service.client.search.SearchIndexingService;
import com.github.NFMdev.cdia.ingestion_service.event.EventCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EventIndexListener {

    private final SearchIndexingService searchIndexingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEventCreated(EventCreatedEvent event) {
        searchIndexingService.index(event.getEvent());
    }
}
