package com.codominator.angusmasail.backend.dto;

import com.codominator.angusmasail.backend.entity.ChatRequestStatus;
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
public class ChatRequestDto {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userLocality;
    private String userPhone;
    private String userManhaj;
    private UUID imamProfileId;
    private String imamName;
    private UUID relatedQueryId;
    private String initialReason;
    private ChatRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
