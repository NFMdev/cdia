package com.github.NFMdev.cdia.search_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "event-anomalies")
public class EventAnomalyDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String location;

    @Field(type = FieldType.Long)
    private long eventCount;

    private String windowStart;

    private String windowEnd;

    private String detectedAt;

    @Field(type = FieldType.Text)
    private String rule;

    @Field(type = FieldType.Keyword)
    private String severity;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Keyword)
    private String alertState;

    @Field(type = FieldType.Text)
    private String description;
}
