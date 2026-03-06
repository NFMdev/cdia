package com.github.NFMdev.cdia.search_service.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import com.github.NFMdev.cdia.common.contract.SearchEventDocument;
import com.github.NFMdev.cdia.search_service.model.AnomalyPageResponse;
import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;
import com.github.NFMdev.cdia.search_service.model.EventDocument;
import com.github.NFMdev.cdia.search_service.repository.EventAnomalyRepository;
import com.github.NFMdev.cdia.search_service.repository.EventDocumentRepository;
import com.github.NFMdev.cdia.search_service.util.AnomalyTimestampNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final List<String> ACTIVE_ALERT_STATES = List.of("OPEN", "ONGOING");
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "detectedAt",
            "eventCount",
            "windowStart",
            "windowEnd",
            "severity",
            "alertState",
            "location"
    );
    private static final String DEFAULT_ANOMALY_TYPE = "INCIDENT";
    private static final String DEFAULT_SORT_FIELD = "detectedAt";
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final EventDocumentRepository eventDocumentRepository;
    private final EventAnomalyRepository eventAnomalyRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public EventDocument index(EventDocument document) {
        return eventDocumentRepository.save(document);
    }

    public EventDocument indexInternal(SearchEventDocument payload) {
        EventDocument document = new EventDocument(
                payload.id(),
                payload.description(),
                payload.location(),
                payload.status(),
                payload.createdAt(),
                payload.sourceSystem(),
                payload.imageUrls(),
                payload.metadata()
        );
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

    public void deleteAllEvents() {
        eventDocumentRepository.deleteAll();
    }

    public List<EventDocument> findAllEvents() {
        List<EventDocument> events = new ArrayList<>();
        eventDocumentRepository.findAll().forEach(events::add);
        return events;
    }

    public void deleteAllAnomalies() {
        eventAnomalyRepository.deleteAll();
    }

    public AnomalyPageResponse findAllAnomalies() {
        return findAllAnomalies(List.of(), List.of(), null, null, 0, DEFAULT_PAGE_SIZE, null, true);
    }

    public AnomalyPageResponse findAllAnomalies(
            List<String> severityFilters,
            List<String> typeFilters,
            String from,
            String to,
            int page,
            int size,
            String sort,
            boolean onlyActive) {
        Query query = buildAnomalyQuery(severityFilters, typeFilters, from, to, onlyActive);
        Pageable pageable = PageRequest.of(sanitizePage(page), sanitizeSize(size), resolveSort(sort));

        SearchHits<EventAnomalyDocument> hits = elasticsearchOperations.search(
                new NativeQueryBuilder()
                        .withQuery(query)
                        .withPageable(pageable)
                        .build(),
                EventAnomalyDocument.class
        );
        if (hits == null) {
            return new AnomalyPageResponse(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, false);
        }

        List<EventAnomalyDocument> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(AnomalyTimestampNormalizer::normalize)
                .toList();

        long total = hits.getTotalHits();
        int normalizedPage = pageable.getPageNumber();
        int normalizedSize = pageable.getPageSize();
        boolean hasNext = ((long) normalizedPage + 1L) * normalizedSize < total;

        return new AnomalyPageResponse(items, normalizedPage, normalizedSize, total, hasNext);
    }

    public List<EventAnomalyDocument> findRecentAnomalies(int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        SearchHits<EventAnomalyDocument> hits = elasticsearchOperations.search(
                new NativeQueryBuilder()
                        .withQuery(Query.of(query -> query.matchAll(matchAll -> matchAll)))
                        .withPageable(PageRequest.of(0, boundedLimit, Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD)))
                        .build(),
                EventAnomalyDocument.class
        );
        if (hits == null) {
            return List.of();
        }

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(AnomalyTimestampNormalizer::normalize)
                .toList();
    }

    public Optional<EventAnomalyDocument> findAnomalyById(String anomalyId) {
        return eventAnomalyRepository.findById(anomalyId)
                .map(AnomalyTimestampNormalizer::normalize);
    }

    private Query buildAnomalyQuery(
            List<String> severityFilters,
            List<String> typeFilters,
            String from,
            String to,
            boolean onlyActive) {
        Instant fromInstant = AnomalyTimestampNormalizer.parseInstant(from);
        Instant toInstant = AnomalyTimestampNormalizer.parseInstant(to);
        Set<String> severitySet = normalizeFilters(severityFilters);
        Set<String> typeSet = normalizeFilters(typeFilters)
                .stream()
                .map(this::normalizeType)
                .collect(java.util.stream.Collectors.toSet());

        List<Query> filters = new ArrayList<>();

        if (onlyActive) {
            filters.add(Query.of(query -> query.terms(terms -> terms
                    .field("alertState")
                    .terms(value -> value.value(ACTIVE_ALERT_STATES.stream().map(FieldValue::of).toList()))
            )));
        }

        if (!severitySet.isEmpty()) {
            filters.add(Query.of(query -> query.terms(terms -> terms
                    .field("severity")
                    .terms(value -> value.value(severitySet.stream().map(FieldValue::of).toList()))
            )));
        }

        if (!typeSet.isEmpty()) {
            filters.add(Query.of(query -> query.terms(terms -> terms
                    .field("type")
                    .terms(value -> value.value(typeSet.stream().map(FieldValue::of).toList()))
            )));
        }

        if (fromInstant != null || toInstant != null) {
            RangeQuery.Builder range = new RangeQuery.Builder().field("detectedAt");
            if (fromInstant != null) {
                range.gte(JsonData.of(fromInstant.toString()));
            }
            if (toInstant != null) {
                range.lte(JsonData.of(toInstant.toString()));
            }
            filters.add(Query.of(query -> query.range(range.build())));
        }

        if (filters.isEmpty()) {
            return Query.of(query -> query.matchAll(matchAll -> matchAll));
        }

        BoolQuery.Builder bool = new BoolQuery.Builder();
        filters.forEach(bool::filter);
        return Query.of(query -> query.bool(bool.build()));
    }

    private Set<String> normalizeFilters(List<String> filters) {
        Set<String> normalized = new HashSet<>();
        for (String value : filters) {
            if (value == null) {
                continue;
            }
            for (String part : value.split(",")) {
                String upper = upper(part);
                if (upper != null) {
                    normalized.add(upper);
                }
            }
        }
        return normalized;
    }

    private String normalizeType(String value) {
        String upper = upper(value);
        return upper == null ? DEFAULT_ANOMALY_TYPE : upper;
    }

    private String upper(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private int sanitizePage(int page) {
        return Math.max(0, page);
    }

    private int sanitizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Sort resolveSort(String sortValue) {
        if (sortValue == null || sortValue.isBlank()) {
            return Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD);
        }

        String[] parts = sortValue.split(":", 2);
        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            field = DEFAULT_SORT_FIELD;
        }

        String directionToken = parts.length > 1 ? parts[1] : "desc";
        Sort.Direction direction = "asc".equalsIgnoreCase(directionToken)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, field);
    }
}
