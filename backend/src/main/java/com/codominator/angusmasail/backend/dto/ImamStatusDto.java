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
public class ImamStatusDto {
    private UUID id;
    private UUID imamProfileId;
    private String contentText;
    private String mediaUrl;
    private String backgroundColorHex;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
