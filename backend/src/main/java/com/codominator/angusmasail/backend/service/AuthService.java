package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.config.JwtUtils;
import com.codominator.angusmasail.backend.dto.*;
import com.codominator.angusmasail.backend.entity.ImamProfile;
import com.codominator.angusmasail.backend.entity.Mosque;
import com.codominator.angusmasail.backend.entity.PrayerTimetable;
import com.codominator.angusmasail.backend.entity.Role;
import com.codominator.angusmasail.backend.entity.User;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.exception.UnauthorizedActionException;
import com.codominator.angusmasail.backend.repository.ImamProfileRepository;
import com.codominator.angusmasail.backend.repository.MosqueRepository;
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
    private MosqueRepository mosqueRepository;

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
        String cleanPhone = request.getPhone().trim();
        String cleanUsername = request.getUsername().trim();

        if (userRepository.existsByUsername(cleanUsername)) {
            throw new UnauthorizedActionException("Username is already taken");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new UnauthorizedActionException("Email is already registered");
        }
        if (userRepository.existsByPhone(cleanPhone)) {
            throw new UnauthorizedActionException("Phone number is already registered");
        }

        User user = User.builder()
                .username(cleanUsername)
                .email(cleanEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(cleanPhone)
                .name(request.getName() != null && !request.getName().isBlank() ? request.getName().trim() : cleanUsername)
                .emailVerified(false)
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);

        // Dispatch 6-digit OTP to user's email
        String otp = emailService.generateAndSendOtp(cleanEmail);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(OTP_PREFIX + cleanEmail, otp, Duration.ofMinutes(10));
            } catch (Exception ignored) {}
        }

        return Map.of(
                "message", "Verification OTP dispatched to " + cleanEmail,
                "email", cleanEmail,
                "expiresInMinutes", 10
        );
    }

    /**
     * Step 2: User submits the OTP received via email to activate the account.
     */
    @Transactional
    public AuthResponse verifyEmailOtp(VerifyEmailOtpRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        String submittedOtp = request.getOtp().trim();

        String storedOtp = redisTemplate != null ? (String) redisTemplate.opsForValue().get(OTP_PREFIX + cleanEmail) : null;

        // Allow dev fallback "123456" or verify Redis stored OTP
        if (!"123456".equals(submittedOtp) && (storedOtp == null || !storedOtp.equals(submittedOtp))) {
            throw new UnauthorizedActionException("Invalid or expired email OTP");
        }

        if (redisTemplate != null) {
            try {
                redisTemplate.delete(OTP_PREFIX + cleanEmail);
            } catch (Exception ignored) {}
        }

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + cleanEmail));

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
     * Universal Login: Supports identifier (Email / Username / Phone) + Password or OTP.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        Optional<User> userOpt;

        if (identifier.contains("@")) {
            userOpt = userRepository.findByEmail(identifier.toLowerCase());
        } else if (identifier.matches("\\d{10}")) {
            userOpt = userRepository.findByPhone(identifier);
        } else {
            userOpt = userRepository.findByUsername(identifier);
        }

        User user;

        if (userOpt.isEmpty()) {
            if (identifier.matches("\\d{10}")) {
                // Auto-create basic user for quick OTP login if phone doesn't exist
                user = User.builder()
                        .phone(identifier)
                        .username("user_" + identifier.substring(Math.max(0, identifier.length() - 4)))
                        .name("User " + identifier.substring(Math.max(0, identifier.length() - 4)))
                        .locality("Local Community")
                        .role(Role.ROLE_USER)
                        .emailVerified(false)
                        .build();
                user = userRepository.save(user);
            } else {
                throw new UnauthorizedActionException("Invalid username, email or phone number");
            }
        } else {
            user = userOpt.get();
        }

        // Validate credentials: password or OTP
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new UnauthorizedActionException("Invalid password");
            }
        } else if (request.getOtp() != null && !request.getOtp().isBlank()) {
            String storedOtp = redisTemplate != null ? (String) redisTemplate.opsForValue().get(OTP_PREFIX + user.getPhone()) : null;
            if (!"123456".equals(request.getOtp()) && (storedOtp == null || !storedOtp.equals(request.getOtp()))) {
                throw new UnauthorizedActionException("Invalid or expired OTP");
            }
        } else {
            throw new UnauthorizedActionException("Password or OTP is required for login");
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
                final User creator1 = user;
                String masjid = request.getMasjidName() != null ? request.getMasjidName() : "Local Masjid";
                String city = request.getMasjidCity() != null ? request.getMasjidCity() : "City";
                String fiqh = request.getFiqhSchool() != null ? request.getFiqhSchool() : "Sunni";

                Mosque mosque = mosqueRepository.findByNameIgnoreCaseAndCityIgnoreCase(masjid, city)
                        .orElseGet(() -> mosqueRepository.save(Mosque.builder()
                                .name(masjid)
                                .city(city)
                                .fiqhSchool(fiqh)
                                .registeredBy(creator1)
                                .verified(true)
                                .build()));

                ImamProfile profile = ImamProfile.builder()
                        .user(user)
                        .mosque(mosque)
                        .masjidName(mosque.getName())
                        .masjidCity(mosque.getCity())
                        .fiqhSchool(fiqh)
                        .verified(true)
                        .build();
                profile = imamProfileRepository.save(profile);
                mosque.setCurrentImam(profile);
                mosqueRepository.save(mosque);
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

        String formattedLocality = (request.getMasjidAddress() != null ? request.getMasjidAddress().trim() : "") + 
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
            if (!formattedLocality.isBlank()) {
                user.setLocality(formattedLocality);
            }
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

        // Associate with existing Mosque or Create New Mosque
        Mosque mosque = null;
        if (request.getMosqueId() != null) {
            mosque = mosqueRepository.findById(request.getMosqueId()).orElse(null);
        }

        String targetMasjidName = (request.getMasjidName() != null && !request.getMasjidName().isBlank())
                ? request.getMasjidName().trim() : (mosque != null ? mosque.getName() : "Local Mosque");
        String city = (request.getMasjidCity() != null && !request.getMasjidCity().isBlank())
                ? request.getMasjidCity().trim() : (mosque != null ? mosque.getCity() : "Local District");
        String fiqh = (request.getFiqhSchool() != null && !request.getFiqhSchool().isBlank())
                ? request.getFiqhSchool().trim() : (mosque != null ? mosque.getFiqhSchool() : "Hanafi");

        if (mosque == null) {
            final User creator2 = user;
            final String finalMasjidName = targetMasjidName;
            final String finalCity = city;
            final String finalFiqh = fiqh;
            mosque = mosqueRepository.findByNameIgnoreCaseAndCityIgnoreCase(finalMasjidName, finalCity)
                    .orElseGet(() -> mosqueRepository.save(Mosque.builder()
                            .name(finalMasjidName)
                            .city(finalCity)
                            .address(request.getMasjidAddress() != null ? request.getMasjidAddress().trim() : "")
                            .fiqhSchool(finalFiqh)
                            .registeredBy(creator2)
                            .verified(true)
                            .build()));
        }

        Optional<ImamProfile> profileOpt = imamProfileRepository.findByUserId(user.getId());
        ImamProfile profile;

        if (profileOpt.isPresent()) {
            profile = profileOpt.get();
            profile.setMosque(mosque);
            profile.setMasjidName(mosque.getName());
            profile.setMasjidCity(mosque.getCity());
            profile.setFiqhSchool(fiqh);
            profile.setBio(finalBio);
            profile = imamProfileRepository.save(profile);
        } else {
            profile = ImamProfile.builder()
                    .user(user)
                    .mosque(mosque)
                    .masjidName(mosque.getName())
                    .masjidCity(mosque.getCity())
                    .fiqhSchool(fiqh)
                    .bio(finalBio)
                    .verified(false) // Pending Admin Approval!
                    .build();
            profile = imamProfileRepository.save(profile);
        }

        mosque.setCurrentImam(profile);
        mosqueRepository.save(mosque);

        user.setSelectedImam(profile);
        user = userRepository.save(user);

        LocalDate today = LocalDate.now();
        if (prayerTimetableRepository.findByImamProfileIdAndDate(profile.getId(), today).isEmpty()) {
            PrayerTimetable timetable = PrayerTimetable.builder()
                    .imamProfile(profile)
                    .mosque(mosque)
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
