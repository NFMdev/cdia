package com.github.NFMdev.cdia.ingestion_service.mapper;

import com.github.NFMdev.cdia.common.dto.EventDto;
import com.github.NFMdev.cdia.ingestion_service.model.event.EventEntity;
import com.github.NFMdev.cdia.ingestion_service.model.source_system.SourceSystemEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventMapperMockitoTest {

    @Mock
    private SourceSystemMapper sourceSystemMapper;

    @Mock
    private ImageMapper imageMapper;

    @Mock
    private AnomalyMapper anomalyMapper;

    @InjectMocks
    private EventMapper eventMapper;

    @Test
    void testToDto() {
        SourceSystemEntity source = new SourceSystemEntity();
        source.setName("CCTV-NorthGate");

        EventEntity event = new EventEntity();
        event.setId(1L);
        event.setDescription("Test Event");
        event.setSourceSystem(source);

        // Mock nested mapper
        when(sourceSystemMapper.toDto(source)).thenReturn("CCTV-NorthGate");

        EventDto dto = eventMapper.toDto(event);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getDescription()).isEqualTo("Test Event");
        assertThat(dto.getSourceSystem()).isEqualTo("CCTV-NorthGate");

        verify(sourceSystemMapper).toDto(source);
    }

    @Test
    void testToEntity() {
        EventDto dto = new EventDto();
        dto.setId(2L);
        dto.setDescription("DTO Event");
        dto.setSourceSystem("MobileApp");

        SourceSystemEntity sourceEntity = new SourceSystemEntity();
        sourceEntity.setName("MobileApp");

        when(sourceSystemMapper.toEntity("MobileApp")).thenReturn(sourceEntity);

        EventEntity entity = eventMapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(2L);
        assertThat(entity.getDescription()).isEqualTo("DTO Event");
        assertThat(entity.getSourceSystem()).isEqualTo(sourceEntity);

        verify(sourceSystemMapper).toEntity("MobileApp");
    }
}
