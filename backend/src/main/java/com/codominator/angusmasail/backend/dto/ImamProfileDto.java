package com.codominator.angusmasail.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImamProfileDto {
    private UUID id;
    private UUID userId;
    private String imamName;
    private String phone;
    private String masjidName;
    private String masjidCity;
    private String bio;
    private String fiqhSchool;
    private Integer hijriOffsetDays;
    private boolean verified;
}
