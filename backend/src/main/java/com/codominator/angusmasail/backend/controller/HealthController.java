package com.codominator.angusmasail.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("Angus Masail Backend API is up and running.");
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, String>> apiHealth() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
