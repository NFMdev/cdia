package com.github.NFMdev.cdia.ingestion_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
class FlywayPostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cdia")
            .withUsername("cdia_app")
            .withPassword("change_me_app");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);

        registry.add("cdia.search.indexing.enabled", () -> "false");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesLatestMigration() {
        String latestVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1",
                String.class
        );

        assertEquals("6", latestVersion);
    }

    @Test
    void eventsRejectNullCreatedAt() {
        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO events (description, location, status, created_at, type) VALUES (?, ?, ?, ?, ?)",
                        "test",
                        "Aalborg",
                        "INGESTED",
                        null,
                        "INCIDENT"
                )
        );

        assertNotNull(exception);
    }

    @Test
    void eventsRejectBlankLocation() {
        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO events (description, location, status, created_at, type) VALUES (?, ?, ?, NOW(), ?)",
                        "test",
                        "   ",
                        "INGESTED",
                        "INCIDENT"
                )
        );

        assertNotNull(exception);
    }
}
