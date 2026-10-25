package com.codominator.angusmasail.backend.dto;

import com.codominator.angusmasail.backend.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {
    @NotBlank(message = "Phone number is required")
    private String phone;

    private String otp;
    private String name;
    private String locality;
    private String manhaj;
    private Role role;
    private String masjidName;
    private String masjidCity;
    private String fiqhSchool;
}
