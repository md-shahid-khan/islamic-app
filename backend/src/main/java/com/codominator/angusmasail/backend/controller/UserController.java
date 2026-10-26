package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.config.JwtUtils;
import com.codominator.angusmasail.backend.config.UserDetailsImpl;
import com.codominator.angusmasail.backend.dto.AuthResponse;
import com.codominator.angusmasail.backend.dto.ImamProfileDto;
import com.codominator.angusmasail.backend.dto.UserDto;
import com.codominator.angusmasail.backend.entity.*;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.repository.*;
import com.codominator.angusmasail.backend.service.AuthService;
import com.codominator.angusmasail.backend.service.LocalFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImamProfileRepository imamProfileRepository;

    @Autowired
    private MosqueRepository mosqueRepository;

    @Autowired
    private MosqueService mosqueService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PrayerTimetableRepository prayerTimetableRepository;

    @Autowired
    private MasailQueryRepository masailQueryRepository;

    @Autowired
    private ChatRequestRepository chatRequestRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ImamResourceRepository imamResourceRepository;

    @Autowired
    private ImamStatusRepository imamStatusRepository;

    @Autowired
    private LocalFileStorageService fileStorageService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    private ImamProfileDto mapToImamProfileDto(ImamProfile p) {
        return ImamProfileDto.builder()
                .id(p.getId())
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .imamName(p.getUser() != null ? p.getUser().getName() : "Scholar")
                .phone(p.getUser() != null ? p.getUser().getPhone() : "")
                .masjidName(p.getMosque() != null ? p.getMosque().getName() : p.getMasjidName())
                .masjidCity(p.getMosque() != null ? p.getMosque().getCity() : p.getMasjidCity())
                .bio(p.getBio())
                .fiqhSchool(p.getFiqhSchool())
                .hijriOffsetDays(p.getHijriOffsetDays())
                .verified(p.isVerified())
                .build();
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userDetails.getId()));
        return ResponseEntity.ok(authService.mapToUserDto(user));
    }

    @GetMapping("/imams")
    public ResponseEntity<List<ImamProfileDto>> searchImams(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fiqhSchool) {
        List<ImamProfile> profiles;
        if (search != null && !search.isBlank()) {
            profiles = imamProfileRepository.findByMasjidCityContainingIgnoreCaseOrMasjidNameContainingIgnoreCase(search, search);
        } else if (fiqhSchool != null && !fiqhSchool.isBlank()) {
            profiles = imamProfileRepository.findByFiqhSchoolContainingIgnoreCase(fiqhSchool);
        } else {
            profiles = imamProfileRepository.findAll();
        }

        List<ImamProfileDto> dtos = profiles.stream().map(this::mapToImamProfileDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping({"/admin/imams/pending", "/admin/pending-imams"})
    public ResponseEntity<List<ImamProfileDto>> getPendingImams() {
        List<ImamProfile> pending = imamProfileRepository.findByVerifiedFalse();
        return ResponseEntity.ok(pending.stream().map(this::mapToImamProfileDto).collect(Collectors.toList()));
    }

    @GetMapping({"/admin/mosques/pending", "/admin/pending-mosques"})
    public ResponseEntity<List<MosqueDto>> getPendingMosques() {
        return ResponseEntity.ok(mosqueService.getPendingMosques());
    }

    @Transactional
    @PutMapping({"/admin/mosques/{mosqueId}/approve", "/admin/pending-mosques/{mosqueId}/approve"})
    public ResponseEntity<MosqueDto> approveMosque(@PathVariable UUID mosqueId) {
        return ResponseEntity.ok(mosqueService.approveMosque(mosqueId));
    }

    @Transactional
    @PutMapping({"/admin/mosques/{mosqueId}/reject", "/admin/pending-mosques/{mosqueId}/reject"})
    public ResponseEntity<Void> rejectMosque(@PathVariable UUID mosqueId) {
        mosqueService.rejectMosque(mosqueId);
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PutMapping({"/admin/imams/{imamProfileId}/approve", "/admin/pending-imams/{imamProfileId}/approve"})
    public ResponseEntity<ImamProfileDto> approveImam(@PathVariable UUID imamProfileId) {
        ImamProfile profile = imamProfileRepository.findById(imamProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Imam profile not found with ID: " + imamProfileId));

        profile.setVerified(true);
        profile = imamProfileRepository.save(profile);

        User user = profile.getUser();
        if (user != null) {
            user.setRole(Role.ROLE_IMAM);
            user.setSelectedImam(profile);
            userRepository.save(user);
        }

        if (profile.getMosque() != null) {
            Mosque m = profile.getMosque();
            m.setCurrentImam(profile);
            mosqueRepository.save(m);
        }

        return ResponseEntity.ok(mapToImamProfileDto(profile));
    }

    @Transactional
    @PutMapping({"/admin/imams/{imamProfileId}/reject", "/admin/pending-imams/{imamProfileId}/reject"})
    public ResponseEntity<Void> rejectImam(@PathVariable UUID imamProfileId) {
        ImamProfile profile = imamProfileRepository.findById(imamProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Imam profile not found with ID: " + imamProfileId));

        purgeImamProfileData(profile);
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @DeleteMapping({"/admin/imams/{imamProfileId}", "/admin/mosques/{imamProfileId}"})
    public ResponseEntity<Void> deleteImamOrMosque(@PathVariable UUID imamProfileId) {
        ImamProfile profile = imamProfileRepository.findById(imamProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Imam profile or mosque not found with ID: " + imamProfileId));

        purgeImamProfileData(profile);
        return ResponseEntity.noContent().build();
    }

    private void purgeImamProfileData(ImamProfile profile) {
        UUID imamProfileId = profile.getId();

        // 0. Unlink from Mosque if associated
        if (profile.getMosque() != null) {
            Mosque m = profile.getMosque();
            m.setCurrentImam(null);
            mosqueRepository.save(m);
            profile.setMosque(null);
        }

        List<Mosque> mosquesWithImam = mosqueRepository.findAll().stream()
                .filter(m -> m.getCurrentImam() != null && m.getCurrentImam().getId().equals(imamProfileId))
                .collect(Collectors.toList());
        for (Mosque m : mosquesWithImam) {
            m.setCurrentImam(null);
            mosqueRepository.save(m);
        }

        // 1. Unlink any users/followers who selected this imam or mosque via direct SQL bulk update
        userRepository.unlinkFollowersByImamProfileId(imamProfileId);

        // 2. Revert the Imam user's role back to standard congregant if applicable
        User user = profile.getUser();
        if (user != null) {
            user.setRole(Role.ROLE_USER);
            user.setSelectedImam(null);
            userRepository.save(user);
        }

        // 3. Delete all ChatMessages and ChatRequests associated with this Imam in single batch queries
        chatMessageRepository.deleteByImamProfileId(imamProfileId);
        chatRequestRepository.deleteByImamProfileId(imamProfileId);

        // 4. Delete all Masail queries for this Imam
        masailQueryRepository.deleteByImamProfileId(imamProfileId);

        // 5. Delete all Prayer Timetables for this Imam
        prayerTimetableRepository.deleteByImamProfileId(imamProfileId);

        // 6. Delete all Imam Resources (books, audio fatwas) and physical files
        List<ImamResource> resources = imamResourceRepository.findByImamProfileIdOrderByCreatedAtDesc(imamProfileId);
        for (ImamResource r : resources) {
            if (r.getFileUrl() != null) fileStorageService.deleteFile(r.getFileUrl());
            if (r.getCoverImageUrl() != null) fileStorageService.deleteFile(r.getCoverImageUrl());
        }
        imamResourceRepository.deleteAll(resources);

        // 7. Delete all Imam Statuses and physical media
        List<ImamStatus> statuses = imamStatusRepository.findByImamProfileId(imamProfileId);
        for (ImamStatus s : statuses) {
            if (s.getMediaUrl() != null) fileStorageService.deleteFile(s.getMediaUrl());
        }
        imamStatusRepository.deleteAll(statuses);

        // 8. Delete the ImamProfile
        imamProfileRepository.delete(profile);

        // 9. Evict cache via exact key deletion and pattern matching
        if (redisTemplate != null) {
            try {
                java.util.Set<String> keys = redisTemplate.keys("prayer:" + imamProfileId + ":*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
                redisTemplate.delete("active-statuses:" + imamProfileId);
            } catch (Exception ignored) {}
        }
    }

    /**
     * When an Imam disassociates from their Mosque:
     * - The Imam is unlinked from the Mosque.
     * - The Mosque remains available in the system for other Imams.
     * - The user is reverted to a normal user (ROLE_USER).
     * - Fresh JWT tokens reflecting ROLE_USER are issued and returned.
     */
    @Transactional
    @PostMapping("/imam/disassociate")
    @PreAuthorize("hasAnyRole('IMAM', 'ADMIN')")
    public ResponseEntity<AuthResponse> disassociateImam(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userDetails.getId()));

        Optional<ImamProfile> profileOpt = imamProfileRepository.findByUserId(user.getId());
        if (profileOpt.isPresent()) {
            ImamProfile profile = profileOpt.get();
            purgeImamProfileData(profile);
        } else {
            user.setRole(Role.ROLE_USER);
            user.setSelectedImam(null);
            user = userRepository.save(user);
        }

        // Generate updated credentials reflecting standard congregant role
        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone(), Role.ROLE_USER.name());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(authService.mapToUserDto(user))
                .build());
    }

    @GetMapping("/users/my-imam-status")
    public ResponseEntity<ImamProfileDto> getMyImamStatus(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.notFound().build();
        }
        Optional<ImamProfile> profileOpt = imamProfileRepository.findByUserId(userDetails.getId());
        return profileOpt.map(imamProfile -> ResponseEntity.ok(mapToImamProfileDto(imamProfile)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/users/select-imam/{imamProfileId}")
    public ResponseEntity<UserDto> selectImam(
            @PathVariable UUID imamProfileId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ImamProfile imamProfile = imamProfileRepository.findById(imamProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Imam profile not found"));

        user.setSelectedImam(imamProfile);
        user = userRepository.save(user);

        return ResponseEntity.ok(authService.mapToUserDto(user));
    }

    @PutMapping("/users/profile")
    public ResponseEntity<UserDto> updateProfile(
            @RequestBody UserDto dto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getLocality() != null) user.setLocality(dto.getLocality());
        if (dto.getCity() != null) user.setCity(dto.getCity());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());
        if (dto.getPreferredLanguage() != null) user.setPreferredLanguage(dto.getPreferredLanguage());
        if (dto.getBio() != null) user.setBio(dto.getBio());
        if (dto.getManhaj() != null) user.setManhaj(dto.getManhaj());
        if (dto.getFcmToken() != null) user.setFcmToken(dto.getFcmToken());

        // Security: Prevent unauthorized role escalation via public profile update
        // Only existing administrators can modify user roles
        if (dto.getRole() != null && user.getRole() == Role.ROLE_ADMIN) {
            user.setRole(Role.fromValue(dto.getRole()));
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(authService.mapToUserDto(user));
    }
}
