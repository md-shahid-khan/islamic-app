package com.codominator.angusmasail.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImamRegistrationRequest {

    @NotBlank(message = "Imam name is required")
    private String imamName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    /**
     * Optional: ID of an existing registered Mosque in the system.
     * If provided, the Imam will be associated with this existing Mosque.
     */
    private UUID mosqueId;

    /**
     * Used if mosqueId is not provided, or to specify new mosque details.
     */
    private String masjidName;

    private String masjidAddress;

    private String masjidCity;

    private String fiqhSchool;

    private String qualification;

    private String bio;

    @Builder.Default
    private String otp = "123456";
}
