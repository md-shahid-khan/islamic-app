package com.codominator.angusmasail.backend.seeder;

import com.codominator.angusmasail.backend.entity.*;
import com.codominator.angusmasail.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private MasailQueryRepository masailQueryRepository;

    @Autowired
    private ImamStatusRepository imamStatusRepository;

    @Autowired
    private ImamResourceRepository imamResourceRepository;

    @Autowired
    private ChatRequestRepository chatRequestRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private MosqueRepository mosqueRepository;

    @Autowired(required = false)
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${app.admin.users:admin@angusmasail.com:admin123:Central Admin:9998887776}")
    private String adminUsersConfig;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Checking database seed status for Angus Masail...");

        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_manhaj_check");
                jdbcTemplate.execute("ALTER TABLE queries_masail DROP CONSTRAINT IF EXISTS queries_masail_manhaj_snapshot_check");
                jdbcTemplate.execute("ALTER TABLE queries_masail DROP CONSTRAINT IF EXISTS masail_queries_manhaj_snapshot_check");
            } catch (Exception ignored) {}
        }

        // 1. Seed Established Mosques (verified by default in directory)
        Mosque mosque1 = mosqueRepository.findByNameIgnoreCaseAndCityIgnoreCase("Masjid Al-Noor", "Central District")
                .orElseGet(() -> mosqueRepository.save(Mosque.builder()
                        .name("Masjid Al-Noor")
                        .city("Central District")
                        .address("Plot 12, Main Sunni Complex")
                        .fiqhSchool("Hanafi")
                        .verified(true)
                        .build()));

        Mosque mosque2 = mosqueRepository.findByNameIgnoreCaseAndCityIgnoreCase("Masjid Bilal (Downtown)", "East District")
                .orElseGet(() -> mosqueRepository.save(Mosque.builder()
                        .name("Masjid Bilal (Downtown)")
                        .city("East District")
                        .address("Near Clock Tower")
                        .fiqhSchool("Shafi'i")
                        .verified(true)
                        .build()));

        Mosque mosque3 = mosqueRepository.findByNameIgnoreCaseAndCityIgnoreCase("Islamic Center of Peace", "North Valley")
                .orElseGet(() -> mosqueRepository.save(Mosque.builder()
                        .name("Islamic Center of Peace")
                        .city("North Valley")
                        .address("Garden Avenue")
                        .fiqhSchool("Hanafi")
                        .verified(true)
                        .build()));

        Mosque mosque4 = mosqueRepository.findByNameIgnoreCaseAndCityIgnoreCase("Masjid Usman Ghani", "West Sector")
                .orElseGet(() -> mosqueRepository.save(Mosque.builder()
                        .name("Masjid Usman Ghani")
                        .city("West Sector")
                        .address("Ring Road, Sector 5")
                        .fiqhSchool("Hanafi")
                        .verified(true)
                        .build()));

        // 2. Seed / Configure Admin Users from Environment / Config
        if (adminUsersConfig != null && !adminUsersConfig.isBlank()) {
            String[] adminEntries = adminUsersConfig.split(",");
            for (String entry : adminEntries) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) continue;
                String[] parts = trimmed.split(":");
                String identifier = parts[0].trim();
                String rawPassword = parts.length > 1 ? parts[1].trim() : "admin123";
                String adminName = parts.length > 2 ? parts[2].trim() : "System Administrator";
                String adminPhone = parts.length > 3 ? parts[3].trim() : "";

                String encodedPassword = passwordEncoder != null ? passwordEncoder.encode(rawPassword) : rawPassword;
                String email = identifier.contains("@") ? identifier : identifier + "@angusmasail.com";
                String username = identifier.contains("@") ? identifier.split("@")[0] : identifier;

                Optional<User> existingUser = userRepository.findByEmail(email);
                if (existingUser.isEmpty() && !adminPhone.isEmpty()) {
                    existingUser = userRepository.findByPhone(adminPhone);
                }
                if (existingUser.isEmpty()) {
                    existingUser = userRepository.findByUsername(username);
                }

                if (existingUser.isPresent()) {
                    User u = existingUser.get();
                    u.setRole(Role.ROLE_ADMIN);
                    u.setPassword(encodedPassword);
                    u.setEmailVerified(true);
                    u.setEmail(email);
                    u.setName(adminName);
                    if (!adminPhone.isEmpty()) {
                        u.setPhone(adminPhone);
                    }
                    userRepository.save(u);
                    System.out.println("Configured Admin user: " + email + " (" + u.getRole() + ")");
                } else {
                    User u = User.builder()
                            .username(username)
                            .email(email)
                            .phone(adminPhone.isEmpty() ? null : adminPhone)
                            .name(adminName)
                            .locality("Central District")
                            .manhaj(Manhaj.HANAFI.getDisplayName())
                            .password(encodedPassword)
                            .emailVerified(true)
                            .role(Role.ROLE_ADMIN)
                            .build();
                    userRepository.save(u);
                    System.out.println("Seeded New Admin user: " + email + " (ROLE_ADMIN)");
                }
            }
        }

        // 3. Seed Home Banners (Max 4)
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

            Banner b3 = Banner.builder()
                    .imageUrl("https://images.unsplash.com/photo-1542816417-0983c9c9ad53?auto=format&fit=crop&w=1200&q=80")
                    .title("Masjid Al-Aqsa • Accurate Daily Prayers & Timetables")
                    .actionUrl(null)
                    .displayOrder(3)
                    .build();

            Banner b4 = Banner.builder()
                    .imageUrl("https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?auto=format&fit=crop&w=1200&q=80")
                    .title("Grand Mosque • Verified Sunni Scholars & Public Library")
                    .actionUrl(null)
                    .displayOrder(4)
                    .build();

            bannerRepository.saveAll(List.of(b1, b2, b3, b4));
        }

        // 4. Seed sample resources if any verified imam exists
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
                        .description("[QURAN] The premier Sunni commentary on Kanzul Iman • Author: Sadrul Afazil Allama Naeemuddin Muradabadi")
                        .fileUrl("https://example.com/books/khazain-ul-irfan.pdf")
                        .build();

                ImamResource r2 = ImamResource.builder()
                        .imamProfile(anyImam)
                        .type(ResourceType.BOOK)
                        .title("Bahar-e-Shariat (بہارِ شریعت جلد دوم - عبادات و روزمرہ مسائل)")
                        .description("[MASAIL] The universal encyclopedia of Hanafi Islamic rulings and daily jurisprudence • Author: Sadrush Shariah Allama Amjad Ali Azmi")
                        .fileUrl("https://example.com/books/bahar-e-shariat.pdf")
                        .build();

                imamResourceRepository.saveAll(List.of(r1, r2));
            }
        }

        System.out.println("✅ Angus Masail Database Seeding Complete! Admins configured from environment.");
    }
}
