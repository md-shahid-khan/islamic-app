package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.dto.CreateMosqueRequest;
import com.codominator.angusmasail.backend.dto.MosqueDto;
import com.codominator.angusmasail.backend.entity.Mosque;
import com.codominator.angusmasail.backend.entity.User;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.repository.MosqueRepository;
import com.codominator.angusmasail.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MosqueService {

    @Autowired
    private MosqueRepository mosqueRepository;

    @Autowired
    private UserRepository userRepository;

    public MosqueDto mapToDto(Mosque m) {
        if (m == null) return null;
        boolean hasImam = m.getCurrentImam() != null;
        return MosqueDto.builder()
                .id(m.getId())
                .name(m.getName())
                .city(m.getCity())
                .address(m.getAddress())
                .country(m.getCountry())
                .fiqhSchool(m.getFiqhSchool())
                .imageUrl(m.getImageUrl())
                .hijriOffsetDays(m.getHijriOffsetDays())
                .verified(m.isVerified())
                .hasImam(hasImam)
                .currentImamId(hasImam ? m.getCurrentImam().getId() : null)
                .currentImamName(hasImam && m.getCurrentImam().getUser() != null ? m.getCurrentImam().getUser().getName() : null)
                .registeredByName(m.getRegisteredBy() != null ? m.getRegisteredBy().getName() : "Community Member")
                .createdAt(m.getCreatedAt())
                .build();
    }

    public List<MosqueDto> getAllMosques(String search, String city) {
        List<Mosque> list;
        if (search != null && !search.isBlank()) {
            list = mosqueRepository.searchVerifiedMosques(search.trim());
        } else {
            list = mosqueRepository.findByVerifiedTrueOrderByCreatedAtDesc();
        }
        return list.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<MosqueDto> getAvailableMosques() {
        return getAvailableMosques(null);
    }

    public List<MosqueDto> getAvailableMosques(UUID userId) {
        List<Mosque> list;
        if (userId != null) {
            list = mosqueRepository.findAvailableMosquesForUser(userId);
        } else {
            list = mosqueRepository.findAvailableMosques();
        }
        return list.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<MosqueDto> getPendingMosques() {
        return mosqueRepository.findByVerifiedFalseOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public MosqueDto getMosqueById(UUID id) {
        Mosque mosque = mosqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque not found with ID: " + id));
        return mapToDto(mosque);
    }

    @Transactional
    public MosqueDto createMosque(UUID userId, CreateMosqueRequest request) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        Mosque mosque = Mosque.builder()
                .name(request.getName().trim())
                .city(request.getCity().trim())
                .address(request.getAddress() != null ? request.getAddress().trim() : "")
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().trim() : "India")
                .fiqhSchool(request.getFiqhSchool() != null && !request.getFiqhSchool().isBlank() ? request.getFiqhSchool().trim() : "Hanafi")
                .imageUrl(request.getImageUrl())
                .hijriOffsetDays(request.getHijriOffsetDays() != null ? request.getHijriOffsetDays() : 0)
                .verified(false) // Requires administrator approval before public visibility
                .registeredBy(user)
                .build();

        mosque = mosqueRepository.save(mosque);
        return mapToDto(mosque);
    }

    @Transactional
    public MosqueDto approveMosque(UUID mosqueId) {
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque not found with ID: " + mosqueId));
        mosque.setVerified(true);
        mosque = mosqueRepository.save(mosque);
        return mapToDto(mosque);
    }

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional
    public void rejectMosque(UUID mosqueId) {
        Mosque mosque = mosqueRepository.findById(mosqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Mosque not found with ID: " + mosqueId));
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.update("UPDATE prayer_timetables SET mosque_id = NULL WHERE mosque_id = ?", mosqueId);
                jdbcTemplate.update("UPDATE imam_profiles SET mosque_id = NULL WHERE mosque_id = ?", mosqueId);
                jdbcTemplate.update("UPDATE users SET selected_mosque_id = NULL WHERE selected_mosque_id = ?", mosqueId);
            } catch (Exception ignored) {}
        }
        mosqueRepository.delete(mosque);
    }
}
