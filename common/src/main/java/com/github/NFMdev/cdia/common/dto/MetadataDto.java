package com.github.NFMdev.cdia.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MetadataDto {
    private Long id;
    private String key;
    private String value;

    private Long eventId;
}
