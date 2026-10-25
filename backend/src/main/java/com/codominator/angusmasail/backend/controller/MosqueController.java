package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.config.UserDetailsImpl;
import com.codominator.angusmasail.backend.dto.CreateMosqueRequest;
import com.codominator.angusmasail.backend.dto.MosqueDto;
import com.codominator.angusmasail.backend.dto.UserDto;
import com.codominator.angusmasail.backend.entity.ImamProfile;
import com.codominator.angusmasail.backend.entity.Mosque;
import com.codominator.angusmasail.backend.entity.User;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.repository.MosqueRepository;
import com.codominator.angusmasail.backend.repository.UserRepository;
import com.codominator.angusmasail.backend.service.AuthService;
import com.codominator.angusmasail.backend.service.MosqueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MosqueController {

    @Autowired
    private MosqueService mosqueService;

    @Autowired
    private MosqueRepository mosqueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    /**
     * Public endpoint: List all registered mosques.
     * No authentication required.
     */
    @GetMapping("/mosques")
    public ResponseEntity<List<MosqueDto>> getMosques(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(mosqueService.getAllMosques(search, city));
    }

    /**
     * Public endpoint: List mosques available for an Imam to register with.
     * No authentication required.
     */
    @GetMapping("/mosques/available")
    public ResponseEntity<List<MosqueDto>> getAvailableMosques() {
        return ResponseEntity.ok(mosqueService.getAvailableMosques());
    }

    /**
     * Public endpoint: Get single mosque by ID.
     */
    @GetMapping("/mosques/{id}")
    public ResponseEntity<MosqueDto> getMosqueById(@PathVariable UUID id) {
        return ResponseEntity.ok(mosqueService.getMosqueById(id));
    }

    /**
     * Authenticated endpoint: Any logged-in user can create/register a Mosque.
     * Authentication required.
     */
    @PostMapping("/mosques")
    public ResponseEntity<MosqueDto> createMosque(
            @Valid @RequestBody CreateMosqueRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(mosqueService.createMosque(userId, request));
    }

    /**
     * Select a mosque. If user is logged in, persists to user account;
     * if guest, returns confirmation DTO.
     */
    @PutMapping("/users/select-mosque/{mosqueId}")
    public ResponseEntity<UserDto> selectMosque(
            @PathVariable UUID mosqueId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque not found with ID: " + mosqueId));

        if (userDetails != null) {
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            ImamProfile imam = mosque.getCurrentImam();
            if (imam != null) {
                user.setSelectedImam(imam);
            }
            user.setCity(mosque.getCity());
            user = userRepository.save(user);
            return ResponseEntity.ok(authService.mapToUserDto(user));
        }

        return ResponseEntity.ok().build();
    }
}
