package com.github.NFMdev.cdia.ingestion_service.mapper;

import com.github.NFMdev.cdia.common.dto.EventDto;
import com.github.NFMdev.cdia.ingestion_service.model.event.EventEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {
        ImageMapper.class,
        AnomalyMapper.class,
        SourceSystemMapper.class
})
public interface EventMapper {

    @AfterMapping
    default void setChildRelations(@MappingTarget EventEntity event, EventDto dto) {
        System.out.println("Setting child relations for event " + event.getId());
        if (event.getImages() != null) {
            event.getImages().forEach(img -> img.setEvent(event));
        }
        if (event.getMetadata() != null) {
            event.getMetadata().forEach(meta -> meta.setEvent(event));
        }
    }

    EventDto toDto(EventEntity eventEntity);

    EventEntity toEntity(EventDto eventDto);

}
