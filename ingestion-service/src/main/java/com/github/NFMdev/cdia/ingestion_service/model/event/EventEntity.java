package com.github.NFMdev.cdia.ingestion_service.model.event;

import com.github.NFMdev.cdia.ingestion_service.model.source_system.SourceSystemEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"images", "metadata"})
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "source_id")
    private SourceSystemEntity sourceSystem;
    
    private String description;
    private String location;
    
//    @Column(columnDefinition = "json")
//    private Map<String, Object> payload;
    
    @Column(length = 20)
    private String status = "INGESTED";
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventImageEntity> images = new ArrayList<>();

    public void addImage(EventImageEntity image) {
        images.add(image);
        image.setEvent(this);
    }

    public void removeImage(EventImageEntity image) {
        images.remove(image);
        image.setEvent(this);
    }
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventMetadataEntity> metadata = new HashSet<>();

    public void addMetadata(EventMetadataEntity meta) {
        metadata.add(meta);
        meta.setEvent(this);
    }

    public void removeMetadata(EventMetadataEntity meta) {
        metadata.remove(meta);
        meta.setEvent(null);
    }
}