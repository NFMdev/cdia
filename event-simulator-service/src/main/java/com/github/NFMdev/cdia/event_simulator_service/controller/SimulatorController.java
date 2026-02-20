package com.github.NFMdev.cdia.event_simulator_service.controller;

import org.springframework.web.bind.annotation.*;

import com.github.NFMdev.cdia.event_simulator_service.simulator.config.SimulatorProperties;
import com.github.NFMdev.cdia.event_simulator_service.simulator.core.SimulatorEngine;

import java.util.Map;

@RestController
@RequestMapping("/simulator")
public class SimulatorController {

    private final SimulatorEngine engine;
    private final SimulatorProperties props;

    public SimulatorController(SimulatorEngine engine, SimulatorProperties props) {
        this.engine = engine;
        this.props = props;
    }

    @PostMapping("/start")
    public Map<String, Object> start() {
        engine.start();
        return status();
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {
        engine.stop();
        return status();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "running", engine.isRunning(),
                "targetUrl", props.getTargetUrl(),
                "eps", props.getEventsPerSecond(),
                "concurrency", props.getConcurrency(),
                "burstEnabled", props.getBurst().isEnabled());
    }
}
