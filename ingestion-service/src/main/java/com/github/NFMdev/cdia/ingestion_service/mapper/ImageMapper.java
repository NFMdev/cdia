package com.github.NFMdev.cdia.ingestion_service.mapper;

import com.github.NFMdev.cdia.common.dto.ImageDto;
import com.github.NFMdev.cdia.ingestion_service.model.event.EventImageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    ImageDto toDto(EventImageEntity entity);
    @Mapping(target = "event", ignore = true)
    EventImageEntity toEntity(ImageDto dto);
}

