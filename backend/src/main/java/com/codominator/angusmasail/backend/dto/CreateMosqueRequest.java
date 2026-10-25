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
public class CreateMosqueRequest {

    @NotBlank(message = "Mosque name is required")
    private String name;

    @NotBlank(message = "City or district is required")
    private String city;

    private String address;

    @Builder.Default
    private String country = "India";

    @Builder.Default
    private String fiqhSchool = "Hanafi";

    private String imageUrl;

    @Builder.Default
    private Integer hijriOffsetDays = 0;
}
