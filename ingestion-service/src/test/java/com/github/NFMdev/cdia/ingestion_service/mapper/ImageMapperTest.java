package com.github.NFMdev.cdia.ingestion_service.mapper;

import com.github.NFMdev.cdia.common.dto.ImageDto;
import com.github.NFMdev.cdia.ingestion_service.model.event.EventImageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled("Enable when running with stable local MapStruct generated classes")
public class ImageMapperTest {

    private ImageMapper imageMapper;

    @BeforeEach
    void setUp() {
        imageMapper = Mappers.getMapper(ImageMapper.class);
    }

    @Test
    void testToEntity() {
        ImageDto dto = new ImageDto();
        dto.setUrl("Test URL");

        EventImageEntity entity = imageMapper.toEntity(dto);

        assertNotNull(entity);
        assertNotNull(entity.getUrl());
        assertEquals(dto.getUrl(), entity.getUrl());
    }

    @Test
    void testToDto() {
        EventImageEntity entity = new EventImageEntity();
        entity.setUrl("Test URL");

        ImageDto dto = imageMapper.toDto(entity);

        assertNotNull(dto);
        assertNotNull(dto.getUrl());
        assertEquals(entity.getUrl(), dto.getUrl());
    }
}
