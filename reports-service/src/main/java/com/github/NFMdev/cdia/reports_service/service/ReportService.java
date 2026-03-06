package com.github.NFMdev.cdia.reports_service.service;

import com.github.NFMdev.cdia.reports_service.model.EventAnomalyDocument;
import com.github.NFMdev.cdia.reports_service.repository.elasticsearch.EventAnomalyRepository;
import com.github.NFMdev.cdia.reports_service.repository.postgres.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportService {

    private final EventRepository eventRepository;
    private final EventAnomalyRepository anomalyRepository;

    public ReportService(EventRepository eventRepository, EventAnomalyRepository anomalyRepository) {
        this.eventRepository = eventRepository;
        this.anomalyRepository = anomalyRepository;
    }

    public Map<String, Object> getEventStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", eventRepository.count());
        stats.put("uniqueLocations", eventRepository.countDistinctLocations());
        stats.put("lastEvent", eventRepository.findTopByOrderByCreatedAtDesc());
        return stats;
    }

    public List<EventAnomalyDocument> getRecentAnomalies() {
        return anomalyRepository.findTop10ByOrderByDetectedAtDesc();
    }

    public Model getDashboardData(Model model) {
        List<EventAnomalyDocument> anomalies = new ArrayList<>();
        anomalyRepository.findAll().forEach(anomalies::add);

        Map<String, Long> locationCountsMap = countBy(anomalies, EventAnomalyDocument::getLocation, "UNKNOWN");
        List<Map.Entry<String, Long>> locationEntries = locationCountsMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .toList();
        List<String> locations = locationEntries.stream()
                .map(Map.Entry::getKey)
                .toList();
        List<Long> eventCounts = locationEntries.stream()
                .map(Map.Entry::getValue)
                .toList();

        Map<String, Long> severityCountsMap = countBy(anomalies, EventAnomalyDocument::getSeverity, "UNKNOWN");
        List<Map.Entry<String, Long>> severityEntries = severityCountsMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .toList();
        List<String> severities = severityEntries.stream()
                .map(Map.Entry::getKey)
                .toList();
        List<Long> severityCounts = severityEntries.stream()
                .map(Map.Entry::getValue)
                .toList();

        model.addAttribute("locations", locations);
        model.addAttribute("eventCounts", eventCounts);
        model.addAttribute("severities", severities);
        model.addAttribute("severityCounts", severityCounts);

        return model;
    }

    private Map<String, Long> countBy(
            List<EventAnomalyDocument> anomalies,
            Function<EventAnomalyDocument, String> classifier,
            String fallback) {
        return anomalies.stream()
                .collect(Collectors.groupingBy(
                        anomaly -> normalize(classifier.apply(anomaly), fallback),
                        Collectors.counting()));
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
