package com.codominator.angusmasail.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerDto {
    private UUID id;
    private String imageUrl;
    private String title;
    private String actionUrl;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
