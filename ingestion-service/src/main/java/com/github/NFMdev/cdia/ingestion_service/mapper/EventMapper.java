package com.github.NFMdev.cdia.ingestion_service.mapper;

import com.github.NFMdev.cdia.common.dto.EventDto;
import com.github.NFMdev.cdia.common.dto.MetadataDto;
import com.github.NFMdev.cdia.ingestion_service.model.event.EventEntity;
import com.github.NFMdev.cdia.ingestion_service.model.event.EventMetadataEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {
        ImageMapper.class,
        SourceSystemMapper.class
})
public interface EventMapper {

    @AfterMapping
    default void setChildRelations(@MappingTarget EventEntity event, EventDto dto) {
        if (event.getImages() != null) {
            event.getImages().forEach(img -> img.setEvent(event));
        }
        if (event.getMetadata() != null) {
            event.getMetadata().forEach(meta -> meta.setEvent(event));
        }
    }

    EventDto toDto(EventEntity eventEntity);

    EventEntity toEntity(EventDto eventDto);

    @Mapping(target = "eventId", source = "event.id")
    MetadataDto toMetadataDto(EventMetadataEntity metadata);

    @Mapping(target = "event", ignore = true)
    EventMetadataEntity toMetadataEntity(MetadataDto metadata);

}
