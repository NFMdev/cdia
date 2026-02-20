package com.github.NFMdev.cdia.ingestion_service.mapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class MockitoConfigurationTest {

    @Test
    void mockCreationWorksWithoutInlineAgent() {
        TestPort port = Mockito.mock(TestPort.class);
        Mockito.when(port.name()).thenReturn("ok");

        assertThat(port.name()).isEqualTo("ok");
    }

    interface TestPort {
        String name();
    }
}
