package com.github.NFMdev.cdia.processing_service.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    public Long id;
    public String type;
    public String description;
    public String location;
    public Long sourceId;
    public Timestamp createdAt;
}
