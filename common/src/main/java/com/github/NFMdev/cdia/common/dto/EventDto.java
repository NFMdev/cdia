package com.github.NFMdev.cdia.common.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class EventDto {
    private Long id;
    @NotBlank
    @Size(max = 50)
    private String type;
    @NotBlank
    private String description;
    @NotBlank
    @Size(max = 255)
    private String location;
    private String status;
    @NotNull
    private LocalDateTime createdAt;
    private Set<MetadataDto> metadata;
    private List<ImageDto> images;
    @NotBlank
    private String sourceSystem;
}
