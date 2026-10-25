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
public class LoginRequest {

    /**
     * Can be email, username, or phone
     */
    @NotBlank(message = "Username, email or phone is required")
    private String identifier;

    private String password;

    private String otp;
}
