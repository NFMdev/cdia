package com.github.NFMdev.cdia.ingestion_service.mapper;

import com.github.NFMdev.cdia.common.dto.AnomalyDto;
import com.github.NFMdev.cdia.ingestion_service.config.MapperConfig;
import com.github.NFMdev.cdia.ingestion_service.model.anomaly.AnomalyEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface AnomalyMapper {
    @Mapping(source = "label.code", target = "label")
    AnomalyDto toDto(AnomalyEntity anomalyEntity);

    @Mapping(source = "label", target = "label.code")
    AnomalyEntity toEntity(AnomalyDto anomalyDto);
}
