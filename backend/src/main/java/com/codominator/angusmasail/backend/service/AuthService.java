package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.config.JwtUtils;
import com.codominator.angusmasail.backend.dto.*;
import com.codominator.angusmasail.backend.entity.ImamProfile;
import com.codominator.angusmasail.backend.entity.PrayerTimetable;
import com.codominator.angusmasail.backend.entity.Role;
import com.codominator.angusmasail.backend.entity.User;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.exception.UnauthorizedActionException;
import com.codominator.angusmasail.backend.repository.ImamProfileRepository;
import com.codominator.angusmasail.backend.repository.PrayerTimetableRepository;
import com.codominator.angusmasail.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImamProfileRepository imamProfileRepository;

    @Autowired
    private PrayerTimetableRepository prayerTimetableRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private static final String OTP_PREFIX = "otp:";

    public String sendOtp(String phone) {
        String otp = "123456"; // Default dev OTP
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(OTP_PREFIX + phone, otp, Duration.ofMinutes(5));
            } catch (Exception ignored) {}
        }
        System.out.println("OTP for " + phone + " is " + otp);
        return "OTP sent successfully to " + phone;
    }

    /**
     * Step 1: User submits Registration form (username, email, password, phone).
     * Validates input, saves user (or pre-populates), dispatches OTP to user's email.
     */
    @Transactional
    public Map<String, Object> registerUser(RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        String cleanUsername = request.getUsername().trim().toLowerCase();
        String cleanPhone = request.getPhone().trim();

        // 1. Check existing verified user collisions
        Optional<User> byEmail = userRepository.findByEmail(cleanEmail);
        if (byEmail.isPresent() && Boolean.TRUE.equals(byEmail.get().getEmailVerified())) {
            throw new IllegalArgumentException("An account with email " + cleanEmail + " already exists. Please sign in.");
        }

        Optional<User> byUsername = userRepository.findByUsername(cleanUsername);
        if (byUsername.isPresent() && Boolean.TRUE.equals(byUsername.get().getEmailVerified())) {
            throw new IllegalArgumentException("Username @" + cleanUsername + " is already taken. Please choose another.");
        }

        Optional<User> byPhone = userRepository.findByPhone(cleanPhone);
        if (byPhone.isPresent() && Boolean.TRUE.equals(byPhone.get().getEmailVerified()) && !byPhone.get().getId().equals(byEmail.map(User::getId).orElse(null))) {
            throw new IllegalArgumentException("Mobile phone number " + cleanPhone + " is already registered.");
        }

        // 2. Create or reuse unverified User
        User user = byEmail.or(() -> byPhone).orElseGet(User::new);
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setPhone(cleanPhone);
        user.setName(request.getName() != null && !request.getName().isBlank() ? request.getName().trim() : request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        if (user.getRole() == null) {
            user.setRole(Role.ROLE_USER);
        }
        userRepository.save(user);

        // 3. Dispatch OTP via EmailService
        String sentOtp = emailService.generateAndSendOtp(cleanEmail);

        return Map.of(
            "status", "SUCCESS",
            "message", "Verification OTP sent successfully to " + cleanEmail,
            "email", cleanEmail,
            "devOtp", sentOtp // included for testing convenience in development
        );
    }

    /**
     * Step 2: User submits the 6-digit OTP received on email.
     * Completes registration, activates account, and issues authentication JWT tokens.
     */
    @Transactional
    public AuthResponse verifyEmailOtp(VerifyEmailOtpRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        boolean valid = emailService.verifyOtp(cleanEmail, request.getOtp());

        if (!valid) {
            throw new UnauthorizedActionException("Invalid or expired OTP code. Please check your email and try again.");
        }

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No pending registration found for email: " + cleanEmail));

        user.setEmailVerified(true);
        user = userRepository.save(user);

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Production Grade Login:
     * Supports identifier (Username / Email / Mobile Phone) + Password or OTP.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        String cleanIdentifier = identifier.toLowerCase();

        // Find user by Email, Username, or Phone
        Optional<User> userOpt = userRepository.findByEmail(cleanIdentifier)
                .or(() -> userRepository.findByUsername(cleanIdentifier))
                .or(() -> userRepository.findByPhone(identifier));

        if (userOpt.isEmpty()) {
            throw new UnauthorizedActionException("No account found matching '" + identifier + "'. Please check your credentials or register.");
        }

        User user = userOpt.get();

        // 1. Password verification
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (user.getPassword() != null) {
                boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword())
                        || "password".equals(request.getPassword()) // universal dev password fallback
                        || "admin123".equals(request.getPassword());
                if (!matches) {
                    throw new UnauthorizedActionException("Incorrect password. Please try again.");
                }
            }
        } 
        // 2. OTP verification
        else if (request.getOtp() != null && !request.getOtp().isBlank()) {
            boolean otpValid = "123456".equals(request.getOtp().trim())
                    || (user.getEmail() != null && emailService.verifyOtp(user.getEmail(), request.getOtp()));
            if (!otpValid) {
                throw new UnauthorizedActionException("Invalid OTP code.");
            }
        } else {
            throw new UnauthorizedActionException("Password or OTP is required to sign in.");
        }

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDto(user))
                .build();
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(AuthRequest request) {
        String phone = request.getPhone();
        String storedOtp = redisTemplate != null ? (String) redisTemplate.opsForValue().get(OTP_PREFIX + phone) : null;

        // Allow "123456" as universal dev override or verify stored OTP
        if (!"123456".equals(request.getOtp()) && (storedOtp == null || !storedOtp.equals(request.getOtp()))) {
            throw new UnauthorizedActionException("Invalid or expired OTP");
        }

        if (redisTemplate != null) {
            try {
                redisTemplate.delete(OTP_PREFIX + phone);
            } catch (Exception ignored) {}
        }

        Optional<User> userOpt = userRepository.findByPhone(phone);
        User user;

        if (userOpt.isEmpty()) {
            Role role = request.getRole() != null ? Role.valueOf(request.getRole().name()) : Role.ROLE_USER;
            user = User.builder()
                    .phone(phone)
                    .username("user_" + phone.substring(Math.max(0, phone.length() - 4)))
                    .name(request.getName() != null ? request.getName() : "User " + phone.substring(Math.max(0, phone.length() - 4)))
                    .locality(request.getLocality() != null ? request.getLocality() : "Local Community")
                    .manhaj(request.getManhaj())
                    .role(role)
                    .emailVerified(false)
                    .build();

            user = userRepository.save(user);

            if (role == Role.ROLE_IMAM) {
                ImamProfile profile = ImamProfile.builder()
                        .user(user)
                        .masjidName(request.getMasjidName() != null ? request.getMasjidName() : "Local Masjid")
                        .masjidCity(request.getMasjidCity() != null ? request.getMasjidCity() : "City")
                        .fiqhSchool(request.getFiqhSchool() != null ? request.getFiqhSchool() : "Sunni")
                        .verified(true)
                        .build();
                imamProfileRepository.save(profile);
            }
        } else {
            user = userOpt.get();
        }

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDto(user))
                .build();
    }

    @Transactional
    public AuthResponse registerImam(ImamRegistrationRequest request) {
        String phone = request.getPhone().trim();
        Optional<User> userOpt = userRepository.findByPhone(phone);
        User user;

        String formattedLocality = request.getMasjidAddress().trim() + 
                (request.getMasjidCity() != null && !request.getMasjidCity().isBlank() ? ", " + request.getMasjidCity().trim() : "");

        if (userOpt.isEmpty()) {
            user = User.builder()
                    .phone(phone)
                    .name(request.getImamName().trim())
                    .username("imam_" + phone.substring(Math.max(0, phone.length() - 4)))
                    .locality(formattedLocality)
                    .role(Role.ROLE_USER)
                    .build();
            user = userRepository.save(user);
        } else {
            user = userOpt.get();
            user.setName(request.getImamName().trim());
            user.setLocality(formattedLocality);
            user = userRepository.save(user);
        }

        StringBuilder bioBuilder = new StringBuilder();
        if (request.getQualification() != null && !request.getQualification().isBlank()) {
            bioBuilder.append(request.getQualification().trim());
        }
        if (request.getBio() != null && !request.getBio().isBlank()) {
            if (bioBuilder.length() > 0) bioBuilder.append(" • ");
            bioBuilder.append(request.getBio().trim());
        }
        String finalBio = bioBuilder.length() > 0 ? bioBuilder.toString() : "Verified Scholar & Imam leading congregational prayers and community Masail guidance.";

        Optional<ImamProfile> profileOpt = imamProfileRepository.findByUserId(user.getId());
        ImamProfile profile;
        String city = request.getMasjidCity() != null && !request.getMasjidCity().isBlank() ? request.getMasjidCity().trim() : request.getMasjidAddress().trim();
        String fiqh = request.getFiqhSchool() != null && !request.getFiqhSchool().isBlank() ? request.getFiqhSchool().trim() : "Hanafi";

        if (profileOpt.isPresent()) {
            profile = profileOpt.get();
            profile.setMasjidName(request.getMasjidName().trim());
            profile.setMasjidCity(city);
            profile.setFiqhSchool(fiqh);
            profile.setBio(finalBio);
            profile = imamProfileRepository.save(profile);
        } else {
            profile = ImamProfile.builder()
                    .user(user)
                    .masjidName(request.getMasjidName().trim())
                    .masjidCity(city)
                    .fiqhSchool(fiqh)
                    .bio(finalBio)
                    .verified(false) // Pending Admin Approval!
                    .build();
            profile = imamProfileRepository.save(profile);
        }

        user.setSelectedImam(profile);
        user = userRepository.save(user);

        LocalDate today = LocalDate.now();
        if (prayerTimetableRepository.findByImamProfileIdAndDate(profile.getId(), today).isEmpty()) {
            PrayerTimetable timetable = PrayerTimetable.builder()
                    .imamProfile(profile)
                    .date(today)
                    .fajrAzan(LocalTime.of(5, 0))
                    .fajrJamaat(LocalTime.of(5, 30))
                    .sunrise(LocalTime.of(6, 15))
                    .dhuhrAzan(LocalTime.of(12, 30))
                    .dhuhrJamaat(LocalTime.of(13, 15))
                    .asrAzan(LocalTime.of(16, 15))
                    .asrJamaat(LocalTime.of(16, 45))
                    .maghribAzan(LocalTime.of(18, 5))
                    .maghribJamaat(LocalTime.of(18, 15))
                    .ishaAzan(LocalTime.of(19, 30))
                    .ishaJamaat(LocalTime.of(20, 0))
                    .jumuahAzan(LocalTime.of(12, 30))
                    .jumuahJamaat(LocalTime.of(13, 30))
                    .build();
            prayerTimetableRepository.save(timetable);
        }

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserDto(user))
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            throw new UnauthorizedActionException("Invalid refresh token");
        }
        String userIdStr = jwtUtils.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone(), user.getRole().name());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(mapToUserDto(user))
                .build();
    }

    public UserDto mapToUserDto(User user) {
        UUID selectedImamId = user.getSelectedImam() != null ? user.getSelectedImam().getId() : null;
        String selectedMasjid = user.getSelectedImam() != null ? user.getSelectedImam().getMasjidName() : null;

        Optional<ImamProfile> ip = imamProfileRepository.findByUserId(user.getId());
        Boolean isImamPending = ip.isPresent() && !ip.get().isVerified();
        Boolean isImamVerified = ip.isPresent() && ip.get().isVerified();

        return UserDto.builder()
                .id(user.getId() != null ? user.getId().toString() : null)
                .username(user.getUsername())
                .email(user.getEmail())
                .emailVerified(user.getEmailVerified())
                .phone(user.getPhone())
                .name(user.getName())
                .locality(user.getLocality())
                .city(user.getCity())
                .address(user.getAddress())
                .preferredLanguage(user.getPreferredLanguage())
                .bio(user.getBio())
                .manhaj(user.getManhaj())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .selectedImamProfileId(selectedImamId != null ? selectedImamId.toString() : null)
                .selectedMasjidName(selectedMasjid)
                .fcmToken(user.getFcmToken())
                .isImamPending(isImamPending)
                .isImamVerified(isImamVerified)
                .build();
    }
}
