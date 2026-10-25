package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.dto.MasailQueryDto;
import com.codominator.angusmasail.backend.entity.*;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.repository.ImamProfileRepository;
import com.codominator.angusmasail.backend.repository.MasailQueryRepository;
import com.codominator.angusmasail.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MasailQueryService {

    @Autowired
    private MasailQueryRepository masailQueryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImamProfileRepository imamProfileRepository;

    @Autowired
    private NotificationService notificationService;

    private static final List<String> URGENT_KEYWORDS = List.of(
            "death", "funeral", "janazah", "emergency", "hospital",
            "grave", "nikah", "talaq", "divorce", "urgent", "critical"
    );

    public List<MasailQueryDto> checkDuplicates(UUID imamProfileId, String questionText) {
        if (questionText == null || questionText.isBlank()) {
            return List.of();
        }
        String[] words = questionText.split("\\s+");
        String firstKeyword = words.length > 0 ? words[0] : questionText;
        List<MasailQuery> faqs = masailQueryRepository.searchPublicFaqs(imamProfileId, firstKeyword, PageRequest.of(0, 3));
        return faqs.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public MasailQueryDto submitQuery(UUID userId, UUID imamProfileId, QueryTag tag, String questionText, String questionAudioUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ImamProfile imamProfile = null;
        if (imamProfileId != null) {
            imamProfile = imamProfileRepository.findById(imamProfileId).orElse(null);
        }
        if (imamProfile == null && user.getSelectedImam() != null) {
            imamProfile = user.getSelectedImam();
        }
        if (imamProfile == null) {
            imamProfile = imamProfileRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No Imam profile found to receive query"));
        }

        user.setSelectedImam(imamProfile);
        userRepository.save(user);

        boolean urgent = isUrgentText(questionText);

        MasailQuery query = MasailQuery.builder()
                .user(user)
                .imamProfile(imamProfile)
                .tag(tag != null ? tag : QueryTag.MASAIL)
                .userNameSnapshot(user.getName())
                .userContactSnapshot(user.getPhone())
                .localitySnapshot(user.getLocality())
                .manhajSnapshot(user.getManhaj())
                .questionText(questionText)
                .questionAudioUrl(questionAudioUrl)
                .isUrgent(urgent)
                .status(QueryStatus.PENDING)
                .build();

        query = masailQueryRepository.save(query);
        return mapToDto(query);
    }

    public List<MasailQueryDto> getUserQueries(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return masailQueryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<MasailQueryDto> getImamQueries(UUID imamUserId, QueryTag tag, QueryStatus status) {
        if (imamUserId == null) {
            return Collections.emptyList();
        }
        ImamProfile imamProfile = imamProfileRepository.findByUserId(imamUserId).orElse(null);
        if (imamProfile == null) {
            // Strictly return empty list for users who are not registered Imams
            return Collections.emptyList();
        }

        List<MasailQuery> queries;
        if (tag != null && status != null) {
            queries = masailQueryRepository.findByImamProfileIdAndTagAndStatusOrderByIsUrgentDescCreatedAtAsc(imamProfile.getId(), tag, status);
        } else if (status != null) {
            queries = masailQueryRepository.findByImamProfileIdAndStatusOrderByIsUrgentDescCreatedAtAsc(imamProfile.getId(), status);
        } else {
            queries = masailQueryRepository.findByImamProfileIdOrderByIsUrgentDescCreatedAtAsc(imamProfile.getId());
        }

        return queries.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public MasailQueryDto acceptQuery(UUID imamUserId, UUID queryId) {
        MasailQuery query = masailQueryRepository.findById(queryId)
                .orElseThrow(() -> new ResourceNotFoundException("Query not found with id: " + queryId));

        if (imamUserId != null) {
            ImamProfile imamProfile = imamProfileRepository.findByUserId(imamUserId).orElse(null);
            if (imamProfile != null && !query.getImamProfile().getId().equals(imamProfile.getId())) {
                throw new AccessDeniedException("Unauthorized to accept query belonging to another Imam");
            }
        }

        query.setStatus(QueryStatus.ACCEPTED);
        query = masailQueryRepository.save(query);

        // Notify user that query was accepted
        try {
            String imamName = (query.getImamProfile() != null && query.getImamProfile().getUser() != null)
                    ? query.getImamProfile().getUser().getName()
                    : "Scholar";
            notificationService.notifyUser(
                    query.getUser().getId(),
                    "Masail Query Accepted",
                    "Imam " + imamName + " has accepted your question! You can continue consultation."
            );
        } catch (Exception ignored) {}

        return mapToDto(query);
    }

    @Transactional
    public MasailQueryDto resolveQuery(UUID imamUserId, UUID queryId, String replyText, String replyAudioUrl, boolean isPublicFaq) {
        MasailQuery query = masailQueryRepository.findById(queryId)
                .orElseThrow(() -> new ResourceNotFoundException("Query not found with id: " + queryId));

        if (imamUserId != null) {
            ImamProfile imamProfile = imamProfileRepository.findByUserId(imamUserId).orElse(null);
            if (imamProfile != null && !query.getImamProfile().getId().equals(imamProfile.getId())) {
                throw new AccessDeniedException("Unauthorized to resolve query belonging to another Imam");
            }
        }

        query.setImamReplyText(replyText);
        query.setImamReplyAudioUrl(replyAudioUrl);
        query.setPublicFaq(isPublicFaq);
        query.setStatus(QueryStatus.RESOLVED);
        query.setResolvedAt(LocalDateTime.now());

        query = masailQueryRepository.save(query);

        // Notify User
        try {
            String imamName = (query.getImamProfile() != null && query.getImamProfile().getUser() != null)
                    ? query.getImamProfile().getUser().getName()
                    : "Scholar";
            notificationService.notifyUser(
                    query.getUser().getId(),
                    "Your Query Has Been Answered",
                    "Imam " + imamName + " has replied to your Masail."
            );
        } catch (Exception ignored) {}

        return mapToDto(query);
    }

    private boolean isUrgentText(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return URGENT_KEYWORDS.stream().anyMatch(lower::contains);
    }

    public MasailQueryDto mapToDto(MasailQuery query) {
        return MasailQueryDto.builder()
                .id(query.getId())
                .userId(query.getUser().getId())
                .imamProfileId(query.getImamProfile().getId())
                .tag(query.getTag())
                .userNameSnapshot(query.getUserNameSnapshot())
                .userContactSnapshot(query.getUserContactSnapshot())
                .localitySnapshot(query.getLocalitySnapshot())
                .manhajSnapshot(query.getManhajSnapshot())
                .questionText(query.getQuestionText())
                .questionAudioUrl(query.getQuestionAudioUrl())
                .isUrgent(query.isUrgent())
                .status(query.getStatus())
                .imamReplyText(query.getImamReplyText())
                .imamReplyAudioUrl(query.getImamReplyAudioUrl())
                .isPublicFaq(query.isPublicFaq())
                .createdAt(query.getCreatedAt())
                .resolvedAt(query.getResolvedAt())
                .build();
    }
}
