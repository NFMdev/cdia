package com.github.NFMdev.cdia.event_simulator_service.simulator.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulator")
public class SimulatorProperties {
    private String targetUrl;
    private int eventsPerSecond = 50;
    private int concurrency = 16;
    private long seed = 42;

    private Burst burst = new Burst();
    private Probabilities probabilities = new Probabilities();
    private List<String> locations = List.of();

    public static class Burst {
        private boolean enabled = false;
        private int everySeconds = 60;
        private int multiplier = 5;
        private int durationSeconds = 10;
        // getters/setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getEverySeconds() { return everySeconds; }
        public void setEverySeconds(int everySeconds) { this.everySeconds = everySeconds; }
        public int getMultiplier() { return multiplier; }
        public void setMultiplier(int multiplier) { this.multiplier = multiplier; }
        public int getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    }

    public static class Probabilities {
        private double metadata = 0.2;
        private double images = 0.03;
        private double anomalies = 0.01;
        // getters/setters
        public double getMetadata() { return metadata; }
        public void setMetadata(double metadata) { this.metadata = metadata; }
        public double getImages() { return images; }
        public void setImages(double images) { this.images = images; }
        public double getAnomalies() { return anomalies; }
        public void setAnomalies(double anomalies) { this.anomalies = anomalies; }
    }

    // getters/setters
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public int getEventsPerSecond() { return eventsPerSecond; }
    public void setEventsPerSecond(int eventsPerSecond) { this.eventsPerSecond = eventsPerSecond; }
    public int getConcurrency() { return concurrency; }
    public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    public Burst getBurst() { return burst; }
    public void setBurst(Burst burst) { this.burst = burst; }
    public Probabilities getProbabilities() { return probabilities; }
    public void setProbabilities(Probabilities probabilities) { this.probabilities = probabilities; }
    public List<String> getLocations() { return locations; }
    public void setLocations(List<String> locations) { this.locations = locations; }
}
