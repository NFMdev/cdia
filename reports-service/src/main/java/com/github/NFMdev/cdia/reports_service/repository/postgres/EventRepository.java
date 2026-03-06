package com.github.NFMdev.cdia.reports_service.repository.postgres;

import com.github.NFMdev.cdia.reports_service.model.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @Query("SELECT COUNT(DISTINCT e.location) FROM EventEntity e")
    long countDistinctLocations();

    EventEntity findTopByOrderByCreatedAtDesc();
}
