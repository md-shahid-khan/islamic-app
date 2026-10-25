package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.dto.ChatMessageDto;
import com.codominator.angusmasail.backend.dto.ChatRequestDto;
import com.codominator.angusmasail.backend.entity.*;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.repository.ChatMessageRepository;
import com.codominator.angusmasail.backend.repository.ChatRequestRepository;
import com.codominator.angusmasail.backend.repository.ImamProfileRepository;
import com.codominator.angusmasail.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatGatekeeperService {

    @Autowired
    private ChatRequestRepository chatRequestRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImamProfileRepository imamProfileRepository;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public ChatRequestDto requestChat(UUID userId, UUID imamProfileId, UUID relatedQueryId, String initialReason) {
        return requestChat(userId, imamProfileId, relatedQueryId, initialReason, null);
    }

    @Transactional
    public ChatRequestDto requestChat(UUID userId, UUID imamProfileId, UUID relatedQueryId, String initialReason, String mediaUrl) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) {
            user = userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No active user found"));
        }

        ImamProfile imamProfile = null;
        if (imamProfileId != null) {
            imamProfile = imamProfileRepository.findById(imamProfileId).orElse(null);
        }
        if (imamProfile == null && user.getSelectedImam() != null) {
            imamProfile = user.getSelectedImam();
        }
        if (imamProfile == null) {
            imamProfile = imamProfileRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No Imam profile available"));
        }

        // Keep user's preferred Imam updated
        user.setSelectedImam(imamProfile);
        userRepository.save(user);

        ChatRequest request = ChatRequest.builder()
                .user(user)
                .imamProfile(imamProfile)
                .relatedQueryId(relatedQueryId)
                .initialReason(initialReason != null && !initialReason.isBlank() ? initialReason : "Consultation request regarding Masail guidance")
                .status(ChatRequestStatus.PENDING)
                .build();

        request = chatRequestRepository.save(request);

        // Record initial consultation message/media in the chat conversation history
        String initialContent = (initialReason != null && !initialReason.isBlank()) ? initialReason : (mediaUrl != null ? "Consultation request attachment" : "Consultation request");
        ChatMessage initialMsg = ChatMessage.builder()
                .chatRequest(request)
                .sender(user)
                .messageText(initialContent)
                .mediaUrl(mediaUrl)
                .sentAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(initialMsg);

        // Notify target Imam
        try {
            if (imamProfile.getUser() != null) {
                notificationService.notifyUser(
                        imamProfile.getUser().getId(),
                        "New Consultation Request",
                        user.getName() + " has requested a 1-on-1 consultation session."
                );
            }
        } catch (Exception ignored) {}

        return mapToDto(request);
    }

    public List<ChatRequestDto> getImamChatRequests(UUID imamUserId, ChatRequestStatus status) {
        if (imamUserId == null) {
            return Collections.emptyList();
        }
        ImamProfile imamProfile = imamProfileRepository.findByUserId(imamUserId).orElse(null);
        if (imamProfile == null) {
            // Strictly return empty list: an Imam should only see their own assigned requests
            return Collections.emptyList();
        }

        List<ChatRequest> requests;
        if (status != null) {
            requests = chatRequestRepository.findByImamProfileIdAndStatusOrderByCreatedAtDesc(imamProfile.getId(), status);
        } else {
            requests = chatRequestRepository.findByImamProfileIdOrderByCreatedAtDesc(imamProfile.getId());
        }

        return requests.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<ChatRequestDto> getUserChatRequests(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return chatRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ChatRequestDto updateRequestStatus(UUID imamUserId, UUID requestId, ChatRequestStatus newStatus) {
        ChatRequest request = chatRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat request not found with id: " + requestId));

        if (imamUserId != null) {
            ImamProfile imamProfile = imamProfileRepository.findByUserId(imamUserId).orElse(null);
            if (imamProfile != null && !request.getImamProfile().getId().equals(imamProfile.getId())) {
                throw new AccessDeniedException("Unauthorized to modify chat request belonging to another Imam");
            }
        }

        request.setStatus(newStatus);
        request.setRespondedAt(LocalDateTime.now());
        request = chatRequestRepository.save(request);

        // Notify User
        try {
            String imamName = (request.getImamProfile() != null && request.getImamProfile().getUser() != null)
                    ? request.getImamProfile().getUser().getName()
                    : "Scholar";
            notificationService.notifyUser(
                    request.getUser().getId(),
                    "Consultation Session " + (newStatus == ChatRequestStatus.ACCEPTED ? "Accepted" : newStatus.name()),
                    "Imam " + imamName + " has accepted your consultation! Open the Chat section to talk."
            );
        } catch (Exception ignored) {}

        return mapToDto(request);
    }

    @Transactional
    public ChatMessageDto saveMessage(UUID chatRequestId, UUID senderId, String messageText, String mediaUrl) {
        ChatRequest request = chatRequestRepository.findById(chatRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat request not found with id: " + chatRequestId));

        if (request.getStatus() == ChatRequestStatus.REJECTED || request.getStatus() == ChatRequestStatus.CLOSED) {
            throw new IllegalStateException("Consultation request is closed or rejected.");
        }

        User sender = null;
        if (senderId != null) {
            sender = userRepository.findById(senderId).orElse(null);
        }
        if (sender == null) {
            sender = request.getUser();
        }

        ChatMessage message = ChatMessage.builder()
                .chatRequest(request)
                .sender(sender)
                .messageText(messageText)
                .mediaUrl(mediaUrl)
                .build();

        message = chatMessageRepository.save(message);

        return ChatMessageDto.builder()
                .id(message.getId())
                .chatRequestId(request.getId())
                .senderId(sender.getId())
                .senderName(sender.getName())
                .messageText(message.getMessageText())
                .mediaUrl(message.getMediaUrl())
                .sentAt(message.getSentAt())
                .build();
    }

    public List<ChatMessageDto> getChatMessages(UUID chatRequestId) {
        return chatMessageRepository.findByChatRequestIdOrderBySentAtAsc(chatRequestId)
                .stream().map(msg -> ChatMessageDto.builder()
                        .id(msg.getId())
                        .chatRequestId(msg.getChatRequest().getId())
                        .senderId(msg.getSender().getId())
                        .senderName(msg.getSender().getName())
                        .messageText(msg.getMessageText())
                        .mediaUrl(msg.getMediaUrl())
                        .sentAt(msg.getSentAt())
                        .readAt(msg.getReadAt())
                        .build())
                .collect(Collectors.toList());
    }

    public ChatRequestDto mapToDto(ChatRequest request) {
        String imamName = (request.getImamProfile() != null && request.getImamProfile().getUser() != null)
                ? request.getImamProfile().getUser().getName()
                : "Masjid Scholar";
        return ChatRequestDto.builder()
                .id(request.getId())
                .userId(request.getUser().getId())
                .userName(request.getUser().getName())
                .userLocality(request.getUser().getLocality())
                .userPhone(request.getUser().getPhone())
                .userManhaj(request.getUser().getManhaj())
                .imamProfileId(request.getImamProfile().getId())
                .imamName(imamName)
                .relatedQueryId(request.getRelatedQueryId())
                .initialReason(request.getInitialReason())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .respondedAt(request.getRespondedAt())
                .build();
    }
}
