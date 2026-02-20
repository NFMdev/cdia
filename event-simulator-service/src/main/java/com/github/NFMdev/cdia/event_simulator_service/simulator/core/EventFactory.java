package com.github.NFMdev.cdia.event_simulator_service.simulator.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final RandomGenerator rng;

    public EventFactory(SimulatorProperties props) {
        this.props = props;
        this.rng = RandomGenerator.getDefault(); // will be reseeded via seed wrapper below
    }

    public EventDto create(long seq, boolean burst, RandomGenerator seededRng) {
        String type = TYPES.get(seededRng.nextInt(TYPES.size()));
        int severity = 1 + seededRng.nextInt(5);

        String location = pickLocation(seededRng);
        double[] latLon = parseLatLon(location);

        EventDto dto = new EventDto();
        dto.setDescription(generateDescription(type, severity, seededRng));
        dto.setLocation(location);
        dto.setStatus("NEW");
        dto.setCreatedAt(LocalDateTime.now().minusNanos(seededRng.nextInt(2_000_000_000)));
        dto.setSourceSystem("SIMULATOR");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", type);
        payload.put("severity", severity);
        payload.put("lat", latLon[0]);
        payload.put("lon", latLon[1]);
        payload.put("tags", pickTags(seededRng));
        payload.put("sequence", seq);

        Map<String, Object> sim = new LinkedHashMap<>();
        sim.put("seed", props.getSeed());
        sim.put("burst", burst);
        sim.put("scenario", "default");
        payload.put("sim", sim);

        dto.setPayload(payload);

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

    private double[] parseLatLon(String location) {
        // expects "... - lat,lon"
        try {
            String[] parts = location.split("-");
            String coords = parts[parts.length - 1].trim();
            String[] xy = coords.split(",");
            return new double[] { Double.parseDouble(xy[0].trim()), Double.parseDouble(xy[1].trim()) };
        } catch (Exception e) {
            return new double[] { 0.0, 0.0 };
        }
    }

    private String generateDescription(String type, int severity, RandomGenerator r) {
        return switch (type) {
            case "ALERT" -> "Alert triggered due to unusual pattern. Severity " + severity + ".";
            case "CRIME" -> "Crime report received from public channel. Severity " + severity + ".";
            default -> "Incident reported near hotspot area. Severity " + severity + ".";
        };
    }

    private List<String> pickTags(RandomGenerator r) {
        int n = 1 + r.nextInt(3);
        Set<String> out = new LinkedHashSet<>();
        while (out.size() < n)
            out.add(TAGS.get(r.nextInt(TAGS.size())));
        return new ArrayList<>(out);
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