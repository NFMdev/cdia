package com.github.NFMdev.cdia.processing_service.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventAnomaly {
    private UUID id;
    private String location;
    public long eventCount;
    public Timestamp windowStart;
    public Timestamp windowEnd;
    public Timestamp detectedAt;
    public String rule;
    public String severity;
    public String alertState;
    public String description;
}
