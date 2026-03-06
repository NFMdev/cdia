package com.github.NFMdev.cdia.search_service.config;

import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnomalyIndexBootstrap {

    private final ElasticsearchOperations elasticsearchOperations;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexAndMapping() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(EventAnomalyDocument.class);
        if (indexOperations == null) {
            log.debug("Skipping anomaly index bootstrap because IndexOperations is unavailable");
            return;
        }
        if (indexOperations.exists()) {
            return;
        }

        boolean created = indexOperations.create();
        if (!created) {
            log.warn("Failed to create anomaly index {}", indexOperations.getIndexCoordinates().getIndexName());
            return;
        }
        indexOperations.putMapping(indexOperations.createMapping(EventAnomalyDocument.class));
        log.info("Created anomaly index {} with explicit mapping", indexOperations.getIndexCoordinates().getIndexName());
    }
}
