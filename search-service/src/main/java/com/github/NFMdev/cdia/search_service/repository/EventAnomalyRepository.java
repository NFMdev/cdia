package com.github.NFMdev.cdia.search_service.repository;

import com.github.NFMdev.cdia.search_service.model.EventAnomalyDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EventAnomalyRepository extends ElasticsearchRepository<EventAnomalyDocument, String> {
    List<EventAnomalyDocument> findByAlertStateIn(Collection<String> alertStates);
}
