package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.dto.BannerDto;
import com.codominator.angusmasail.backend.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<BannerDto>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getBanners());
    }

    @PostMapping
    public ResponseEntity<?> createBanner(@RequestBody Map<String, String> request) {
        String imageUrl = request.get("imageUrl");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "imageUrl is required"));
        }
        String title = request.get("title");
        String actionUrl = request.get("actionUrl");

        try {
            BannerDto created = bannerService.createBanner(imageUrl, title, actionUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBanner(@PathVariable UUID id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(Map.of("message", "Banner deleted successfully"));
    }
}
