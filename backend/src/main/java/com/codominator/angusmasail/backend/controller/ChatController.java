package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.config.UserDetailsImpl;
import com.codominator.angusmasail.backend.dto.ChatMessageDto;
import com.codominator.angusmasail.backend.dto.ChatRequestDto;
import com.codominator.angusmasail.backend.entity.ChatRequestStatus;
import com.codominator.angusmasail.backend.service.ChatGatekeeperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    @Autowired
    private ChatGatekeeperService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.codominator.angusmasail.backend.service.LocalFileStorageService fileStorageService;

    @PostMapping("/chat/request")
    public ResponseEntity<ChatRequestDto> requestChat(
            @RequestParam(required = false) UUID relatedQueryId,
            @RequestParam(required = false) UUID imamProfileId,
            @RequestParam String initialReason,
            @RequestParam(required = false) String mediaUrl,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(chatService.requestChat(userId, imamProfileId, relatedQueryId, initialReason, mediaUrl));
    }

    @GetMapping("/chat/my-requests")
    public ResponseEntity<List<ChatRequestDto>> getMyChatRequests(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(chatService.getUserChatRequests(userId));
    }

    @GetMapping("/imam/chat/requests")
    @PreAuthorize("hasAnyRole('IMAM', 'ADMIN')")
    public ResponseEntity<List<ChatRequestDto>> getImamChatRequests(
            @RequestParam(required = false) ChatRequestStatus status,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID imamUserId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(chatService.getImamChatRequests(imamUserId, status));
    }

    @PostMapping("/imam/chat/{requestId}/accept")
    @PreAuthorize("hasAnyRole('IMAM', 'ADMIN')")
    public ResponseEntity<ChatRequestDto> acceptChat(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID imamUserId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(chatService.updateRequestStatus(imamUserId, requestId, ChatRequestStatus.ACCEPTED));
    }

    @PostMapping("/imam/chat/{requestId}/reject")
    @PreAuthorize("hasAnyRole('IMAM', 'ADMIN')")
    public ResponseEntity<ChatRequestDto> rejectChat(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID imamUserId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(chatService.updateRequestStatus(imamUserId, requestId, ChatRequestStatus.REJECTED));
    }

    @PostMapping("/imam/chat/{requestId}/close")
    @PreAuthorize("hasAnyRole('IMAM', 'ADMIN')")
    public ResponseEntity<ChatRequestDto> closeChat(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID imamUserId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(chatService.updateRequestStatus(imamUserId, requestId, ChatRequestStatus.CLOSED));
    }

    @GetMapping("/chat/{requestId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getChatMessages(@PathVariable UUID requestId) {
        return ResponseEntity.ok(chatService.getChatMessages(requestId));
    }

    @PostMapping("/chat/{requestId}/messages")
    public ResponseEntity<ChatMessageDto> postMessage(
            @PathVariable UUID requestId,
            @RequestParam String messageText,
            @RequestParam(required = false) String mediaUrl,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID senderId = userDetails != null ? userDetails.getId() : null;
        ChatMessageDto saved = chatService.saveMessage(requestId, senderId, messageText, mediaUrl);
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + requestId, saved);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/chat/upload")
    public ResponseEntity<?> uploadAttachment(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty"));
        }

        long maxSizeBytes = 15 * 1024 * 1024; // 15 MB limit
        if (file.getSize() > maxSizeBytes) {
            return ResponseEntity.badRequest().body(Map.of("error", "File exceeds 15 MB limit"));
        }

        try {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
            String storedUrl = fileStorageService.storeFile(file, "chat");

            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String fileType = "FILE";
            if (contentType.startsWith("image/") || originalName.matches(".*(?i)\\.(jpg|jpeg|png|webp|gif)$")) {
                fileType = "IMAGE";
            } else if (contentType.equals("application/pdf") || originalName.toLowerCase().endsWith(".pdf")) {
                fileType = "PDF";
            } else if (contentType.startsWith("audio/") || originalName.matches(".*(?i)\\.(mp3|m4a|wav)$")) {
                fileType = "AUDIO";
            } else if (contentType.startsWith("video/") || originalName.matches(".*(?i)\\.(mp4|mkv|mov|webm|avi)$")) {
                fileType = "VIDEO";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("url", storedUrl);
            response.put("fileName", originalName);
            response.put("fileType", fileType);
            response.put("fileSize", file.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to store file: " + e.getMessage()));
        }
    }

    @MessageMapping("/chat.send/{requestId}")
    public void sendMessage(
            @DestinationVariable UUID requestId,
            @Payload ChatMessageDto dto,
            Principal principal) {
        UUID senderId = principal != null ? UUID.fromString(principal.getName()) : null;
        ChatMessageDto saved = chatService.saveMessage(requestId, senderId, dto.getMessageText(), dto.getMediaUrl());
        messagingTemplate.convertAndSend("/topic/chat/" + requestId, saved);
    }
}
