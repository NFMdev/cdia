package com.github.NFMdev.cdia.ingestion_service.mapper;

import com.github.NFMdev.cdia.common.dto.AnomalyDto;
import com.github.NFMdev.cdia.ingestion_service.model.anomaly.AnomalyEntity;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AnomalyMapperTest {

    private AnomalyMapper anomalyMapper;

    @BeforeEach
    void setUp() {
        anomalyMapper = org.mapstruct.factory.Mappers.getMapper(AnomalyMapper.class);
    }

    void testToEntity() {
        AnomalyDto dto = new AnomalyDto();
        dto.setDescription( "Test anomaly" );

        AnomalyEntity entity = anomalyMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(dto.getDescription(), entity.getDescription());
    }

    void testToDto() {
        AnomalyEntity entity = new AnomalyEntity();
        entity.setDescription("Test anomaly");

        AnomalyDto dto = anomalyMapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(entity.getDescription(), dto.getDescription());

    }
}
