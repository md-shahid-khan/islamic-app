package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.dto.*;
import com.codominator.angusmasail.backend.config.UserDetailsImpl;
import com.codominator.angusmasail.backend.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestParam String phone) {
        return ResponseEntity.ok(authService.sendOtp(phone));
    }

    /**
     * Step 1 of Production Registration:
     * Receives username, email, password, phone -> Dispatches 6-digit OTP to user's email.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerUser(request));
    }

    /**
     * Step 2 of Production Registration:
     * Receives email + 6-digit OTP received via email -> Activates account and returns JWT credentials.
     */
    @PostMapping("/verify-email-otp")
    public ResponseEntity<AuthResponse> verifyEmailOtp(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(authService.verifyEmailOtp(request));
    }

    /**
     * Production Login:
     * Supports identifier (Email / Username / Phone) + Password or OTP.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.verifyOtpAndLogin(request));
    }

    @PostMapping("/register-imam")
    public ResponseEntity<AuthResponse> registerImam(
            @Valid @RequestBody ImamRegistrationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(authService.registerImam(request, userDetails));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
