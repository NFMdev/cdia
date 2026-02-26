package com.github.NFMdev.cdia.search_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "events")
public class EventDocument {

    @Id
    private String id;

    private String description;
    private String location;
    private String status;
    private LocalDateTime createdAt;

    private String sourceSystem;

    @Field(type = FieldType.Nested)
    private List<String> imageUrls;

    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;


}
