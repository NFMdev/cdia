package com.github.NFMdev.cdia.ingestion_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@Disabled("Enable when running with stable local MapStruct/Spring test wiring")
class EventServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
