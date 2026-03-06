package com.github.NFMdev.cdia.processing_service.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventAnomaly {
    private String id;
    private String location;
    public long eventCount;
    public String windowStart;
    public String windowEnd;
    public String detectedAt;
    public String rule;
    public String severity;
    public String type;
    public String alertState;
    public String description;
    public Long firstEventId;
    public Long lastEventId;
}
