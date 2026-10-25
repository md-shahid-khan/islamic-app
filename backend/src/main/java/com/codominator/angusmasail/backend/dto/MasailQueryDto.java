package com.codominator.angusmasail.backend.dto;

import com.codominator.angusmasail.backend.entity.QueryStatus;
import com.codominator.angusmasail.backend.entity.QueryTag;
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
public class MasailQueryDto {
    private UUID id;
    private UUID userId;
    private UUID imamProfileId;
    private QueryTag tag;
    private String userNameSnapshot;
    private String userContactSnapshot;
    private String localitySnapshot;
    private String manhajSnapshot;
    private String questionText;
    private String questionAudioUrl;
    private boolean isUrgent;
    private QueryStatus status;
    private String imamReplyText;
    private String imamReplyAudioUrl;
    private boolean isPublicFaq;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
