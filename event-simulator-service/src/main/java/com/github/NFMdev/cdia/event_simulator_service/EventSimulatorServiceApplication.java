package com.github.NFMdev.cdia.event_simulator_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.github.NFMdev.cdia.event_simulator_service.simulator.config.SimulatorProperties;

@SpringBootApplication
@EnableConfigurationProperties(SimulatorProperties.class)
public class EventSimulatorServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(EventSimulatorServiceApplication.class, args);
    }
}
