package com.codominator.angusmasail.backend.dto;

import com.codominator.angusmasail.backend.entity.ResourceType;
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
public class ImamResourceDto {
    private UUID id;
    private UUID imamProfileId;
    private ResourceType type;
    private String title;
    private String description;
    private String fileUrl;
    private String coverImageUrl;
    private LocalDateTime createdAt;
}
