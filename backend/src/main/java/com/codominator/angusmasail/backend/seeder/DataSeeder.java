package com.codominator.angusmasail.backend.seeder;

import com.codominator.angusmasail.backend.entity.*;
import com.codominator.angusmasail.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImamProfileRepository imamProfileRepository;

    @Autowired
    private PrayerTimetableRepository prayerTimetableRepository;

    @Autowired
    private ImamResourceRepository imamResourceRepository;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private MosqueRepository mosqueRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Value("${app.admin.users:admin@angusmasail.com:Admin@123:Super Admin:9999999999}")
    private String adminUsersConfig;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // =========================================================================
        // 1. Mandatory Database Schema Constraint Fixes (Production & Dev)
        // =========================================================================
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_manhaj_check");
                jdbcTemplate.execute("ALTER TABLE queries_masail DROP CONSTRAINT IF EXISTS queries_masail_manhaj_snapshot_check");
                jdbcTemplate.execute("ALTER TABLE queries_masail DROP CONSTRAINT IF EXISTS masail_queries_manhaj_snapshot_check");
            } catch (Exception ignored) {}
        }

        // =========================================================================
        // 2. Mandatory Admin User Provisioning (Required in all profiles from .env)
        // =========================================================================
        seedAdminUsers();

        // =========================================================================
        // 3. In Production: Skip all mock/sample data seeding
        // =========================================================================
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            System.out.println("🚀 Production profile active. Admin & schema ready. Skipping mock data seeding.");
            return;
        }

        // =========================================================================
        // 4. Local Development Only: Seed Mock Data for UI Testing
        // =========================================================================
        seedDevMockData();
    }

    private void seedAdminUsers() {
        String rawConfig = resolveAdminConfig();
        if (rawConfig == null || rawConfig.isBlank()) {
            System.out.println("ℹ️ No admin configuration found in environment. Using default admin template.");
            rawConfig = "admin@angusmasail.com:Admin@123:Super Admin:9999999999";
        }

        System.out.println("🌱 Processing Admin provisioning from config: " + maskSensitiveConfig(rawConfig));

        String[] adminEntries = rawConfig.split(",");
        for (String entry : adminEntries) {
            String trimmed = cleanValue(entry);
            if (trimmed.isEmpty()) continue;

            try {
                processSingleAdminEntry(trimmed);
            } catch (Exception e) {
                System.err.println("❌ Failed to provision admin entry '" + trimmed + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void processSingleAdminEntry(String entry) {
        String[] parts = entry.split(":");
        String rawIdentifier = cleanValue(parts[0]);
        String rawPassword = parts.length > 1 ? cleanValue(parts[1]) : "Admin@123";
        String adminName = parts.length > 2 ? cleanValue(parts[2]) : "Super Admin";
        String adminPhone = parts.length > 3 ? cleanValue(parts[3]) : "";

        if (rawIdentifier.isEmpty()) return;

        // Determine email and username
        String email;
        String username;

        if (rawIdentifier.contains("@")) {
            email = rawIdentifier.toLowerCase();
            username = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");
        } else {
            // Identifier is a username
            username = rawIdentifier.toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_");
            email = username + "@angusmasail.com";
        }

        // Generate a safe unique 10-digit phone if not provided to satisfy not-null constraint
        String cleanPhone = adminPhone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() < 7 || cleanPhone.length() > 15) {
            cleanPhone = "99" + String.format("%08d", Math.abs(email.hashCode()) % 100000000);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Find existing user by email, username, or phone
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByUsername(username);
        }
        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByPhone(cleanPhone);
        }

        User u;
        if (existingUser.isPresent()) {
            u = existingUser.get();
            u.setRole(Role.ROLE_ADMIN);
            u.setPassword(encodedPassword);
            u.setEmailVerified(true);
            u.setEmail(email);
            u.setName(adminName);

            // Avoid phone collision if phone belongs to another account
            Optional<User> phoneHolder = userRepository.findByPhone(cleanPhone);
            if (phoneHolder.isEmpty() || phoneHolder.get().getId().equals(u.getId())) {
                u.setPhone(cleanPhone);
            }

            // Avoid username collision
            Optional<User> userHolder = userRepository.findByUsername(username);
            if (userHolder.isEmpty() || userHolder.get().getId().equals(u.getId())) {
                u.setUsername(username);
            }

            u = userRepository.save(u);
            printAdminSuccess("Updated Existing Admin", u);
        } else {
            // Check collisions before insert
            Optional<User> phoneHolder = userRepository.findByPhone(cleanPhone);
            if (phoneHolder.isPresent()) {
                cleanPhone = "98" + String.format("%08d", (System.currentTimeMillis() + Math.abs(email.hashCode())) % 100000000);
            }

            Optional<User> userHolder = userRepository.findByUsername(username);
            if (userHolder.isPresent()) {
                username = username + "_" + (System.currentTimeMillis() % 1000);
            }

            u = User.builder()
                    .username(username)
                    .email(email)
                    .phone(cleanPhone)
                    .name(adminName)
                    .locality("Central District")
                    .manhaj(Manhaj.HANAFI.getDisplayName())
                    .password(encodedPassword)
                    .emailVerified(true)
                    .role(Role.ROLE_ADMIN)
                    .build();

            u = userRepository.save(u);
            printAdminSuccess("Created New Admin", u);
        }
    }

    private void printAdminSuccess(String action, User u) {
        System.out.println("==================================================================");
        System.out.println("👑 [ADMIN SEEDER] " + action + ":");
        System.out.println("   📧 Email:    " + u.getEmail());
        System.out.println("   👤 Username: " + u.getUsername());
        System.out.println("   📱 Phone:    " + u.getPhone());
        System.out.println("   📛 Name:     " + u.getName());
        System.out.println("   🛡️ Role:     " + u.getRole());
        System.out.println("   🔑 Password: [BCRYPT ENCRYPTED]");
        System.out.println("==================================================================");
    }

    private String resolveAdminConfig() {
        // 1. Direct environment variable APP_ADMIN_USERS / app.admin.users
        String config = getFirstNonBlank(
                environment.getProperty("APP_ADMIN_USERS"),
                environment.getProperty("app.admin.users"),
                environment.getProperty("ADMIN_USERS"),
                adminUsersConfig
        );

        if (config != null && !config.isBlank()) {
            return cleanValue(config);
        }

        // 2. Individual environment variables (ADMIN_EMAIL + ADMIN_PASSWORD)
        String email = getFirstNonBlank(
                environment.getProperty("ADMIN_EMAIL"),
                environment.getProperty("APP_ADMIN_EMAIL")
        );
        String password = getFirstNonBlank(
                environment.getProperty("ADMIN_PASSWORD"),
                environment.getProperty("APP_ADMIN_PASSWORD")
        );

        if (email != null && password != null) {
            String name = getFirstNonBlank(
                    environment.getProperty("ADMIN_NAME"),
                    environment.getProperty("APP_ADMIN_NAME"),
                    "Super Admin"
            );
            String phone = getFirstNonBlank(
                    environment.getProperty("ADMIN_PHONE"),
                    environment.getProperty("APP_ADMIN_PHONE"),
                    "9999999999"
            );
            return cleanValue(email) + ":" + cleanValue(password) + ":" + cleanValue(name) + ":" + cleanValue(phone);
        }

        return null;
    }

    private String getFirstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private String cleanValue(String val) {
        if (val == null) return "";
        val = val.trim();
        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            if (val.length() >= 2) {
                val = val.substring(1, val.length() - 1).trim();
            }
        }
        return val;
    }

    private String maskSensitiveConfig(String config) {
        if (config == null || config.isBlank()) return "[EMPTY]";
        return config.replaceAll(":[^:,]+:", ":****:");
    }

    private void seedDevMockData() {
        // Seed Established Mosques (Dev only)
        Mosque mosque1 = mosqueRepository.findByNameIgnoreCaseAndCityIgnoreCase("Masjid Al-Noor", "Central District")
                .orElseGet(() -> mosqueRepository.save(Mosque.builder()
                        .name("Masjid Al-Noor")
                        .city("Central District")
                        .address("Plot 12, Main Sunni Complex")
                        .fiqhSchool("Hanafi")
                        .verified(true)
                        .build()));

        mosqueRepository.findByNameIgnoreCaseAndCityIgnoreCase("Masjid Bilal (Downtown)", "East District")
                .orElseGet(() -> mosqueRepository.save(Mosque.builder()
                        .name("Masjid Bilal (Downtown)")
                        .city("East District")
                        .address("Near Clock Tower")
                        .fiqhSchool("Shafi'i")
                        .verified(true)
                        .build()));

        // Seed Home Banners (Dev only)
        if (bannerRepository.count() == 0) {
            Banner b1 = Banner.builder()
                    .imageUrl("https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?auto=format&fit=crop&w=1200&q=80")
                    .title("Masjid an-Nabawi • Welcome to Angus Masail")
                    .actionUrl(null)
                    .displayOrder(1)
                    .build();

            Banner b2 = Banner.builder()
                    .imageUrl("https://images.unsplash.com/photo-1564769625905-50e93615e769?auto=format&fit=crop&w=1200&q=80")
                    .title("Holy Kaaba • Connect with Your Local Mosque")
                    .actionUrl(null)
                    .displayOrder(2)
                    .build();

            bannerRepository.saveAll(List.of(b1, b2));
        }

        // Seed sample resources if any verified imam exists (Dev only)
        ImamProfile anyImam = imamProfileRepository.findAll().stream().filter(ImamProfile::isVerified).findFirst().orElse(null);
        if (anyImam != null) {
            LocalDate today = LocalDate.now();
            if (prayerTimetableRepository.findByImamProfileIdAndDate(anyImam.getId(), today).isEmpty()) {
                PrayerTimetable t1 = PrayerTimetable.builder()
                        .imamProfile(anyImam)
                        .mosque(anyImam.getMosque() != null ? anyImam.getMosque() : mosque1)
                        .date(today)
                        .fajrAzan(LocalTime.of(4, 30))
                        .fajrJamaat(LocalTime.of(5, 0))
                        .sunrise(LocalTime.of(6, 15))
                        .dhuhrAzan(LocalTime.of(12, 30))
                        .dhuhrJamaat(LocalTime.of(13, 15))
                        .asrAzan(LocalTime.of(16, 15))
                        .asrJamaat(LocalTime.of(16, 45))
                        .maghribAzan(LocalTime.of(18, 45))
                        .maghribJamaat(LocalTime.of(18, 55))
                        .ishaAzan(LocalTime.of(20, 0))
                        .ishaJamaat(LocalTime.of(20, 30))
                        .jumuahAzan(LocalTime.of(12, 30))
                        .jumuahJamaat(LocalTime.of(13, 30))
                        .build();
                prayerTimetableRepository.save(t1);
            }

            if (imamResourceRepository.count() == 0) {
                ImamResource r1 = ImamResource.builder()
                        .imamProfile(anyImam)
                        .type(ResourceType.BOOK)
                        .title("Tafseer Khazain-ul-Irfan (خزائن العرفان فی تفسیر القرآن)")
                        .description("[QURAN] The premier Sunni commentary on Kanzul Iman")
                        .fileUrl("https://example.com/books/khazain-ul-irfan.pdf")
                        .build();

                imamResourceRepository.save(r1);
            }
        }
    }
}
