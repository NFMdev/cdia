package com.github.NFMdev.cdia.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AnomalyDto {
    private String id;
    private String label;
    private String description;
    private LocalDateTime detectedAt;
    private Double confidenceScore;
    private String severity;
    private Long firstEventId;
    private Long lastEventId;
}
