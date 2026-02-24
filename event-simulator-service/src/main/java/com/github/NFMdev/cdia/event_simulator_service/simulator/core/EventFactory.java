package com.github.NFMdev.cdia.event_simulator_service.simulator.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import com.github.NFMdev.cdia.common.dto.EventDto;
import com.github.NFMdev.cdia.common.dto.ImageDto;
import com.github.NFMdev.cdia.common.dto.MetadataDto;
import com.github.NFMdev.cdia.event_simulator_service.simulator.config.SimulatorProperties;



public class EventFactory {
    private static final List<String> TYPES = List.of("INCIDENT", "ALERT", "CRIME");
    private static final List<String> TAGS = List.of("night", "public", "traffic", "theft", "suspicious", "vandalism");

    private final SimulatorProperties props;

    public EventFactory(SimulatorProperties props) {
        this.props = props;
    }

    public EventDto create(long seq, boolean burst, RandomGenerator seededRng) {
        String type = TYPES.get(seededRng.nextInt(TYPES.size()));
        int severity = 1 + seededRng.nextInt(5);

        String location = pickLocation(seededRng);

        EventDto dto = new EventDto();
        dto.setDescription(generateDescription(type, severity, seededRng));
        dto.setLocation(location);
        dto.setStatus("NEW");
        dto.setCreatedAt(LocalDateTime.now().minusNanos(seededRng.nextInt(2_000_000_000)));
        dto.setSourceSystem("SIMULATOR");

        dto.setMetadata(maybeMetadata(seededRng));
        dto.setImages(maybeImages(seededRng));

        return dto;
    }

    public RandomGenerator forTick(long tick) {
        // reproducible per tick/sequence
        return new SplittableRandom(props.getSeed() ^ (tick * 31_415_926_535L));
    }

    private String pickLocation(RandomGenerator r) {
        List<String> locs = props.getLocations();
        if (locs == null || locs.isEmpty())
            return "Unknown - 0.0,0.0";
        return locs.get(r.nextInt(locs.size()));
    }

    private String generateDescription(String type, int severity, RandomGenerator r) {
        return switch (type) {
            case "ALERT" -> "Alert triggered due to unusual pattern. Severity " + severity + ".";
            case "CRIME" -> "Crime report received from public channel. Severity " + severity + ".";
            default -> "Incident reported near hotspot area. Severity " + severity + ".";
        };
    }

    private Set<MetadataDto> maybeMetadata(RandomGenerator r) {
        if (r.nextDouble() > props.getProbabilities().getMetadata())
            return Set.of();
        return Set.of(
                new MetadataDto(null, "channel", List.of("phone", "web", "sensor").get(r.nextInt(3)), null),
                new MetadataDto(null, "reporter", List.of("anonymous", "citizen", "agent").get(r.nextInt(3)), null));
    }

    private List<ImageDto> maybeImages(RandomGenerator r) {
        if (r.nextDouble() > props.getProbabilities().getImages())
            return List.of();
        return List
                .of(new ImageDto(null, "https://example.com/img/" + r.nextInt(10_000) + ".jpg"));
    }
}
