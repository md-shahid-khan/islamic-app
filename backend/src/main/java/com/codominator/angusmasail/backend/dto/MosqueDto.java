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
public class MosqueDto {
    private UUID id;
    private String name;
    private String city;
    private String address;
    private String country;
    private String fiqhSchool;
    private String imageUrl;
    private Integer hijriOffsetDays;
    private boolean verified;
    private boolean hasImam;
    private UUID currentImamId;
    private String currentImamName;
    private String registeredByName;
    private LocalDateTime createdAt;
}
