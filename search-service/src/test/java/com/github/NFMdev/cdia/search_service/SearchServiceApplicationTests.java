package com.github.NFMdev.cdia.search_service;

import com.github.NFMdev.cdia.search_service.repository.EventDocumentRepository;
import com.github.NFMdev.cdia.search_service.repository.EventAnomalyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class SearchServiceApplicationTests {

	@MockBean
	private EventDocumentRepository eventDocumentRepository;

	@MockBean
	private EventAnomalyRepository eventAnomalyRepository;

	@Test
	void contextLoads() {
	}

}
