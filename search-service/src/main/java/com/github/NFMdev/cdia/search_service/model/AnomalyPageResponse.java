package com.github.NFMdev.cdia.search_service.model;

import java.util.List;

public record AnomalyPageResponse(
        List<EventAnomalyDocument> items,
        int page,
        int size,
        long total,
        boolean hasNext) {
}
