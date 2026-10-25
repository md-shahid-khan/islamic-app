package com.codominator.angusmasail.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private UUID id;
    private UUID chatRequestId;
    private UUID senderId;
    private String senderName;
    private String messageText;
    private String mediaUrl;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
