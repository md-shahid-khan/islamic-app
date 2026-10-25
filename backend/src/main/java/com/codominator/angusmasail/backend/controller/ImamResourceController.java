package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.config.UserDetailsImpl;
import com.codominator.angusmasail.backend.dto.ImamResourceDto;
import com.codominator.angusmasail.backend.dto.ImamStatusDto;
import com.codominator.angusmasail.backend.entity.ImamProfile;
import com.codominator.angusmasail.backend.entity.ImamResource;
import com.codominator.angusmasail.backend.entity.ResourceType;
import com.codominator.angusmasail.backend.repository.ImamProfileRepository;
import com.codominator.angusmasail.backend.repository.ImamResourceRepository;
import com.codominator.angusmasail.backend.service.ImamResourceService;
import com.codominator.angusmasail.backend.service.LocalFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class ImamResourceController {

    @Autowired
    private ImamResourceService resourceService;

    @Autowired
    private LocalFileStorageService fileStorageService;

    @Autowired
    private ImamProfileRepository imamProfileRepository;

    @Autowired
    private ImamResourceRepository imamResourceRepository;

    @PostMapping(value = "/imam/resources", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasRole('IMAM')")
    public ResponseEntity<ImamResourceDto> createResource(
            @RequestParam ResourceType type,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestPart(value = "resourceFile", required = false) MultipartFile resourceFile,
            @RequestPart(value = "coverImageFile", required = false) MultipartFile coverImageFile,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String fileUrl = null;
        if (resourceFile != null && !resourceFile.isEmpty()) {
            fileUrl = fileStorageService.storeFile(resourceFile, "books");
        }

        String coverImageUrl = null;
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            coverImageUrl = fileStorageService.storeFile(coverImageFile, "books");
        }

        return ResponseEntity.ok(resourceService.createResource(userDetails.getId(), type, title, description, fileUrl, coverImageUrl));
    }

    @GetMapping("/resources")
    public ResponseEntity<List<ImamResourceDto>> getResources(
            @RequestParam(value = "imamId", required = false) UUID imamId,
            @RequestParam(value = "imamProfileId", required = false) UUID imamProfileId,
            @RequestParam(required = false) ResourceType type) {
        UUID targetId = imamId != null ? imamId : imamProfileId;
        if (targetId == null) {
            return ResponseEntity.ok(resourceService.getAllLibraryResources());
        }
        return ResponseEntity.ok(resourceService.getResources(targetId, type));
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<Void> deleteResource(
            @PathVariable UUID resourceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        resourceService.deleteResource(resourceId, userDetails != null ? userDetails.getId() : null);
        return ResponseEntity.noContent().build();
    }

    // --- Public Islamic Library APIs ---
    @GetMapping("/library/books")
    public ResponseEntity<List<ImamResourceDto>> getLibraryBooks(
            @RequestParam(required = false) String category) {
        List<ImamResourceDto> all = resourceService.getAllLibraryResources();
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            String catClean = category.trim().toLowerCase();
            all = all.stream().filter(r -> {
                String desc = r.getDescription() != null ? r.getDescription().toLowerCase() : "";
                String title = r.getTitle() != null ? r.getTitle().toLowerCase() : "";
                return desc.contains(catClean) || title.contains(catClean);
            }).collect(Collectors.toList());
        }
        return ResponseEntity.ok(all);
    }

    @DeleteMapping("/library/books/{bookId}")
    public ResponseEntity<Void> deleteLibraryBook(
            @PathVariable UUID bookId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        resourceService.deleteResource(bookId, userDetails != null ? userDetails.getId() : null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/library/books")
    public ResponseEntity<ImamResourceDto> createLibraryBook(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String title = payload.getOrDefault("title", "Islamic Book");
        String author = payload.getOrDefault("author", "");
        String category = payload.getOrDefault("category", "MASAIL");
        String fileUrl = payload.getOrDefault("fileUrl", "");
        String description = payload.getOrDefault("description", "Authentic Islamic book resource.");

        UUID imamProfileId = null;
        if (userDetails != null) {
            Optional<ImamProfile> p = imamProfileRepository.findByUserId(userDetails.getId());
            if (p.isPresent()) imamProfileId = p.get().getId();
        }
        if (imamProfileId == null) {
            List<ImamProfile> all = imamProfileRepository.findAll();
            if (!all.isEmpty()) imamProfileId = all.get(0).getId();
        }

        ImamProfile profile = imamProfileId != null ? imamProfileRepository.findById(imamProfileId).orElse(null) : null;
        String fullDescription = "[" + category.toUpperCase() + "] " + description + (author.isBlank() ? "" : " • Author: " + author);

        ImamResource r = ImamResource.builder()
                .imamProfile(profile)
                .type(ResourceType.BOOK)
                .title(title)
                .description(fullDescription)
                .fileUrl(fileUrl)
                .coverImageUrl(null)
                .build();

        r = imamResourceRepository.save(r);
        return ResponseEntity.ok(resourceService.mapToDto(r));
    }

    @PostMapping(value = "/imam/status", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasRole('IMAM')")
    public ResponseEntity<ImamStatusDto> createStatus(
            @RequestParam(required = false) String contentText,
            @RequestParam(required = false) String backgroundColorHex,
            @RequestPart(value = "mediaFile", required = false) MultipartFile mediaFile,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String mediaUrl = null;
        if (mediaFile != null && !mediaFile.isEmpty()) {
            mediaUrl = fileStorageService.storeFile(mediaFile, "statuses");
        }

        return ResponseEntity.ok(resourceService.createStatus(userDetails.getId(), contentText, mediaUrl, backgroundColorHex));
    }

    @GetMapping("/status/active")
    public ResponseEntity<List<ImamStatusDto>> getActiveStatuses(
            @RequestParam(value = "imamId", required = false) UUID imamId,
            @RequestParam(value = "imamProfileId", required = false) UUID imamProfileId) {
        UUID targetId = imamId != null ? imamId : imamProfileId;
        if (targetId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(resourceService.getActiveStatuses(targetId));
    }
}
