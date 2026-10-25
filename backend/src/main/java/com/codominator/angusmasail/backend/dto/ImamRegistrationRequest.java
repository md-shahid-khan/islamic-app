package com.codominator.angusmasail.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImamRegistrationRequest {

    @NotBlank(message = "Imam name is required")
    private String imamName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Mosque name is required")
    private String masjidName;

    @NotBlank(message = "Mosque address is required")
    private String masjidAddress;

    private String masjidCity;

    private String fiqhSchool;

    private String qualification;

    private String bio;

    @Builder.Default
    private String otp = "123456";
}
