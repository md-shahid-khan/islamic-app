package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.dto.ImamResourceDto;
import com.codominator.angusmasail.backend.dto.ImamStatusDto;
import com.codominator.angusmasail.backend.entity.*;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.exception.UnauthorizedActionException;
import com.codominator.angusmasail.backend.repository.ImamProfileRepository;
import com.codominator.angusmasail.backend.repository.ImamResourceRepository;
import com.codominator.angusmasail.backend.repository.ImamStatusRepository;
import com.codominator.angusmasail.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImamResourceService {

    @Autowired
    private ImamResourceRepository imamResourceRepository;

    @Autowired
    private ImamStatusRepository imamStatusRepository;

    @Autowired
    private ImamProfileRepository imamProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocalFileStorageService fileStorageService;

    @Transactional
    public ImamResourceDto createResource(UUID imamUserId, ResourceType type, String title, String description, String fileUrl, String coverImageUrl) {
        ImamProfile imamProfile = imamProfileRepository.findByUserId(imamUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Imam profile not found"));

        ImamResource resource = ImamResource.builder()
                .imamProfile(imamProfile)
                .type(type != null ? type : ResourceType.BOOK)
                .title(title)
                .description(description)
                .fileUrl(fileUrl)
                .coverImageUrl(coverImageUrl)
                .build();

        resource = imamResourceRepository.save(resource);

        return mapToDto(resource);
    }

    public List<ImamResourceDto> getResources(UUID imamProfileId, ResourceType type) {
        if (imamProfileId == null) {
            return getAllLibraryResources();
        }
        List<ImamResource> resources;
        if (type != null) {
            resources = imamResourceRepository.findByImamProfileIdAndTypeOrderByCreatedAtDesc(imamProfileId, type);
        } else {
            resources = imamResourceRepository.findByImamProfileIdOrderByCreatedAtDesc(imamProfileId);
        }

        return resources.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<ImamResourceDto> getAllLibraryResources() {
        return imamResourceRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public void deleteResource(UUID resourceId, UUID userId) {
        ImamResource resource = imamResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            boolean isAdmin = user != null && user.getRole() == Role.ROLE_ADMIN;
            boolean isOwner = resource.getImamProfile() != null &&
                    resource.getImamProfile().getUser() != null &&
                    resource.getImamProfile().getUser().getId().equals(userId);
            if (!isAdmin && !isOwner) {
                throw new UnauthorizedActionException("Only administrators or the authoring Imam can delete this book resource");
            }
        }

        if (resource.getFileUrl() != null) {
            fileStorageService.deleteFile(resource.getFileUrl());
        }
        if (resource.getCoverImageUrl() != null) {
            fileStorageService.deleteFile(resource.getCoverImageUrl());
        }

        imamResourceRepository.delete(resource);
    }

    @Transactional
    public ImamStatusDto createStatus(UUID imamUserId, String contentText, String mediaUrl, String backgroundColorHex) {
        ImamProfile imamProfile = imamProfileRepository.findByUserId(imamUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Imam profile not found"));

        ImamStatus status = ImamStatus.builder()
                .imamProfile(imamProfile)
                .contentText(contentText)
                .mediaUrl(mediaUrl)
                .backgroundColorHex(backgroundColorHex != null ? backgroundColorHex : "#0A4D3C")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        status = imamStatusRepository.save(status);

        return mapToDto(status);
    }

    public List<ImamStatusDto> getActiveStatuses(UUID imamProfileId) {
        return imamStatusRepository.findByImamProfileIdAndExpiresAtAfterOrderByCreatedAtDesc(imamProfileId, LocalDateTime.now())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupExpiredStatuses() {
        List<ImamStatus> expired = imamStatusRepository.findByExpiresAtBefore(LocalDateTime.now());
        if (!expired.isEmpty()) {
            imamStatusRepository.deleteAll(expired);
        }
    }

    public ImamResourceDto mapToDto(ImamResource resource) {
        return ImamResourceDto.builder()
                .id(resource.getId())
                .imamProfileId(resource.getImamProfile() != null ? resource.getImamProfile().getId() : null)
                .type(resource.getType())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .fileUrl(resource.getFileUrl())
                .coverImageUrl(resource.getCoverImageUrl())
                .createdAt(resource.getCreatedAt())
                .build();
    }

    public ImamStatusDto mapToDto(ImamStatus status) {
        return ImamStatusDto.builder()
                .id(status.getId())
                .imamProfileId(status.getImamProfile() != null ? status.getImamProfile().getId() : null)
                .contentText(status.getContentText())
                .mediaUrl(status.getMediaUrl())
                .backgroundColorHex(status.getBackgroundColorHex())
                .createdAt(status.getCreatedAt())
                .expiresAt(status.getExpiresAt())
                .build();
    }
}
