package com.github.NFMdev.cdia.common.contract;

import com.github.NFMdev.cdia.common.dto.EventDto;
import com.github.NFMdev.cdia.common.dto.ImageDto;
import com.github.NFMdev.cdia.common.dto.MetadataDto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SearchEventDocument(
        String id,
        String description,
        String location,
        String status,
        LocalDateTime createdAt,
        String sourceSystem,
        List<String> imageUrls,
        Map<String, Object> metadata) {

    public static SearchEventDocument fromEvent(EventDto event) {
        return new SearchEventDocument(
                event.getId() == null ? null : event.getId().toString(),
                event.getDescription(),
                event.getLocation(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getSourceSystem(),
                mapImages(event.getImages()),
                mapMetadata(event.getMetadata())
        );
    }

    private static List<String> mapImages(List<ImageDto> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .map(ImageDto::getUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    private static Map<String, Object> mapMetadata(Set<MetadataDto> metadata) {
        if (metadata == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (MetadataDto item : metadata) {
            if (item.getKey() == null || item.getKey().isBlank()) {
                continue;
            }
            result.put(item.getKey(), item.getValue());
        }
        return result;
    }
}
