package com.codominator.angusmasail.backend.seeder;

import com.codominator.angusmasail.backend.entity.*;
import com.codominator.angusmasail.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

    @Autowired(required = false)
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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

        // 1. Seed or get Imam 1: Imam Syed Ahmed
        User imam1User = userRepository.findByPhone("1234567890").orElseGet(() -> {
            User u = User.builder()
                    .phone("1234567890")
                    .name("Imam Syed Ahmed")
                    .locality("Central District")
                    .manhaj(Manhaj.HANAFI.getDisplayName())
                    .role(Role.ROLE_IMAM)
                    .build();
            return userRepository.save(u);
        });

        ImamProfile imam1Profile = imamProfileRepository.findByUserId(imam1User.getId())
                .orElseGet(() -> {
                    ImamProfile p = ImamProfile.builder()
                            .user(imam1User)
                            .masjidName("Masjid Al-Noor")
                            .masjidCity("Central District")
                            .bio("Senior Sunni Scholar serving Masjid Al-Noor for over 15 years. Specializes in Hanafi jurisprudence and community counseling.")
                            .fiqhSchool("Hanafi")
                            .hijriOffsetDays(0)
                            .verified(true)
                            .build();
                    return imamProfileRepository.save(p);
                });

        // 2. Seed Imam 2: Sheikh Bilal Mustafa
        User imam2User = userRepository.findByPhone("1122334455").orElseGet(() -> {
            User u = User.builder()
                    .phone("1122334455")
                    .name("Sheikh Bilal Mustafa")
                    .locality("East District")
                    .manhaj(Manhaj.SHAFII.getDisplayName())
                    .role(Role.ROLE_IMAM)
                    .build();
            return userRepository.save(u);
        });

        ImamProfile imam2Profile = imamProfileRepository.findByUserId(imam2User.getId())
                .orElseGet(() -> {
                    ImamProfile p = ImamProfile.builder()
                            .user(imam2User)
                            .masjidName("Masjid Bilal (Downtown)")
                            .masjidCity("East District")
                            .bio("Graduate of Al-Azhar, leading congregational prayers, youth tajweed classes and weekly Hadith circles.")
                            .fiqhSchool("Shafi'i")
                            .hijriOffsetDays(0)
                            .verified(true)
                            .build();
                    return imamProfileRepository.save(p);
                });

        // 3. Seed Imam 3: Mufti Abdul Rahman
        User imam3User = userRepository.findByPhone("5566778899").orElseGet(() -> {
            User u = User.builder()
                    .phone("5566778899")
                    .name("Mufti Abdul Rahman")
                    .locality("North Valley")
                    .manhaj(Manhaj.HANAFI.getDisplayName())
                    .role(Role.ROLE_IMAM)
                    .build();
            return userRepository.save(u);
        });

        ImamProfile imam3Profile = imamProfileRepository.findByUserId(imam3User.getId())
                .orElseGet(() -> {
                    ImamProfile p = ImamProfile.builder()
                            .user(imam3User)
                            .masjidName("Islamic Center of Peace")
                            .masjidCity("North Valley")
                            .bio("Mufti and educator providing comprehensive rulings, marriage counseling and inheritance arbitration.")
                            .fiqhSchool("Hanafi")
                            .hijriOffsetDays(0)
                            .verified(true)
                            .build();
                    return imamProfileRepository.save(p);
                });

        // 4. Seed Regular Users
        User regularUser1 = userRepository.findByPhone("0987654321").orElseGet(() -> {
            User u = User.builder()
                    .phone("0987654321")
                    .name("Tariq Mansoor")
                    .locality("Central District")
                    .manhaj(Manhaj.HANAFI.getDisplayName())
                    .role(Role.ROLE_USER)
                    .selectedImam(imam1Profile)
                    .build();
            return userRepository.save(u);
        });

        User regularUser2 = userRepository.findByPhone("9876543210").orElseGet(() -> {
            User u = User.builder()
                    .phone("9876543210")
                    .name("Zayd Al-Ansari")
                    .locality("North Area")
                    .manhaj(Manhaj.HANAFI.getDisplayName())
                    .role(Role.ROLE_USER)
                    .selectedImam(imam1Profile)
                    .build();
            return userRepository.save(u);
        });

        // 4b. Seed Admin User
        userRepository.findByPhone("9998887776").orElseGet(() -> {
            User u = User.builder()
                    .phone("9998887776")
                    .username("admin")
                    .email("admin@angusmasail.com")
                    .name("Central Mosque Admin")
                    .locality("Central District")
                    .manhaj(Manhaj.HANAFI.getDisplayName())
                    .password(passwordEncoder != null ? passwordEncoder.encode("admin123") : "admin123")
                    .emailVerified(true)
                    .role(Role.ROLE_ADMIN)
                    .build();
            return userRepository.save(u);
        });

        // 4c. Seed Pending Imam Applicant for Admin approval queue
        User pendingImamUser = userRepository.findByPhone("7778889990").orElseGet(() -> {
            User u = User.builder()
                    .phone("7778889990")
                    .username("farhan_qasmi")
                    .email("farhan@masjidtaqwa.org")
                    .name("Maulana Farhan Qasmi")
                    .locality("West Suburb")
                    .manhaj(Manhaj.HANAFI.getDisplayName())
                    .password(passwordEncoder != null ? passwordEncoder.encode("password") : "password")
                    .emailVerified(true)
                    .role(Role.ROLE_USER)
                    .build();
            return userRepository.save(u);
        });

        if (imamProfileRepository.findByVerifiedFalse().isEmpty()) {
            ImamProfile pendingProfile = ImamProfile.builder()
                    .user(pendingImamUser)
                    .masjidName("Masjid Al-Taqwa")
                    .masjidCity("West Suburb")
                    .bio("Experienced Alim offering daily prayers and adult Quran classes. Awaiting admin board approval.")
                    .fiqhSchool("Hanafi")
                    .hijriOffsetDays(0)
                    .verified(false)
                    .build();
            imamProfileRepository.save(pendingProfile);
        }

        // 5. Seed Today's Prayer Timetable for Imam 1
        LocalDate today = LocalDate.now();
        if (prayerTimetableRepository.findByImamProfileIdAndDate(imam1Profile.getId(), today).isEmpty()) {
            PrayerTimetable t1 = PrayerTimetable.builder()
                    .imamProfile(imam1Profile)
                    .date(today)
                    .fajrStartTime(LocalTime.of(4, 30))
                    .fajrEndTime(LocalTime.of(6, 15))
                    .fajrAzan(LocalTime.of(4, 30))
                    .fajrJamaat(LocalTime.of(5, 0))
                    .sunriseStartTime(LocalTime.of(6, 15))
                    .sunriseEndTime(LocalTime.of(12, 30))
                    .sunrise(LocalTime.of(6, 15))
                    .dhuhrStartTime(LocalTime.of(12, 30))
                    .dhuhrEndTime(LocalTime.of(16, 15))
                    .dhuhrAzan(LocalTime.of(12, 30))
                    .dhuhrJamaat(LocalTime.of(13, 15))
                    .asrStartTime(LocalTime.of(16, 15))
                    .asrEndTime(LocalTime.of(18, 45))
                    .asrAzan(LocalTime.of(16, 15))
                    .asrJamaat(LocalTime.of(16, 45))
                    .maghribStartTime(LocalTime.of(18, 45))
                    .maghribEndTime(LocalTime.of(20, 0))
                    .maghribAzan(LocalTime.of(18, 45))
                    .maghribJamaat(LocalTime.of(18, 55))
                    .ishaStartTime(LocalTime.of(20, 0))
                    .ishaEndTime(LocalTime.of(4, 30))
                    .ishaAzan(LocalTime.of(20, 0))
                    .ishaJamaat(LocalTime.of(20, 30))
                    .jumuahStartTime(LocalTime.of(12, 30))
                    .jumuahEndTime(LocalTime.of(16, 15))
                    .jumuahAzan(LocalTime.of(12, 30))
                    .jumuahJamaat(LocalTime.of(13, 30))
                    .build();
            prayerTimetableRepository.save(t1);
        }

        // Prayer Timetable for Imam 2
        if (prayerTimetableRepository.findByImamProfileIdAndDate(imam2Profile.getId(), today).isEmpty()) {
            PrayerTimetable t2 = PrayerTimetable.builder()
                    .imamProfile(imam2Profile)
                    .date(today)
                    .fajrStartTime(LocalTime.of(4, 25))
                    .fajrEndTime(LocalTime.of(6, 10))
                    .fajrAzan(LocalTime.of(4, 25))
                    .fajrJamaat(LocalTime.of(4, 55))
                    .sunriseStartTime(LocalTime.of(6, 10))
                    .sunriseEndTime(LocalTime.of(12, 30))
                    .sunrise(LocalTime.of(6, 10))
                    .dhuhrStartTime(LocalTime.of(12, 30))
                    .dhuhrEndTime(LocalTime.of(16, 20))
                    .dhuhrAzan(LocalTime.of(12, 30))
                    .dhuhrJamaat(LocalTime.of(13, 0))
                    .asrStartTime(LocalTime.of(16, 20))
                    .asrEndTime(LocalTime.of(18, 45))
                    .asrAzan(LocalTime.of(16, 20))
                    .asrJamaat(LocalTime.of(16, 50))
                    .maghribStartTime(LocalTime.of(18, 45))
                    .maghribEndTime(LocalTime.of(20, 5))
                    .maghribAzan(LocalTime.of(18, 45))
                    .maghribJamaat(LocalTime.of(18, 55))
                    .ishaStartTime(LocalTime.of(20, 5))
                    .ishaEndTime(LocalTime.of(4, 25))
                    .ishaAzan(LocalTime.of(20, 5))
                    .ishaJamaat(LocalTime.of(20, 25))
                    .jumuahStartTime(LocalTime.of(12, 30))
                    .jumuahEndTime(LocalTime.of(16, 20))
                    .jumuahAzan(LocalTime.of(12, 30))
                    .jumuahJamaat(LocalTime.of(13, 15))
                    .build();
            prayerTimetableRepository.save(t2);
        }

        // 6. Seed Islamic Resources & Books
        if (imamResourceRepository.count() == 0) {
            System.out.println("Seeding authentic Islamic resources & books...");
            ImamResource r1 = ImamResource.builder()
                    .imamProfile(imam1Profile)
                    .type(ResourceType.BOOK)
                    .title("Tafseer Khazain-ul-Irfan (خزائن العرفان فی تفسیر القرآن)")
                    .description("[QURAN] The premier Sunni commentary on Kanzul Iman • Author: Sadrul Afazil Allama Naeemuddin Muradabadi")
                    .fileUrl("https://example.com/books/khazain-ul-irfan.pdf")
                    .coverImageUrl(null)
                    .build();

            ImamResource r2 = ImamResource.builder()
                    .imamProfile(imam1Profile)
                    .type(ResourceType.BOOK)
                    .title("Mirat-ul-Manajih Sharh Mishkat-ul-Masabih (مرآۃ المناجیح)")
                    .description("[HADITH] Comprehensive 8-volume Hadith commentary & rulings • Author: Mufti Ahmad Yaar Khan Naeemi")
                    .fileUrl("https://example.com/books/mirat-ul-manajih.pdf")
                    .coverImageUrl(null)
                    .build();

            ImamResource r3 = ImamResource.builder()
                    .imamProfile(imam1Profile)
                    .type(ResourceType.BOOK)
                    .title("Al-Mu'taqad al-Muntaqad (المعتقد المنتقد فی عقائد اہلسنت)")
                    .description("[AQAID] Fundamental creed of Ahle Sunnat Wal Jama'at • Author: Ala Hazrat Imam Ahmad Raza Khan Barelvi")
                    .fileUrl("https://example.com/books/al-mutaqad.pdf")
                    .coverImageUrl(null)
                    .build();

            ImamResource r4 = ImamResource.builder()
                    .imamProfile(imam2Profile)
                    .type(ResourceType.BOOK)
                    .title("Namaz-e-Mustafa Sallallahu Alaihi Wasallam (نمازِ مصطفیٰ)")
                    .description("[NAMAZ] Complete guide to Salah, Wudu, Taharah, and Sunnah practices with authentic proofs.")
                    .fileUrl("https://example.com/books/namaz-e-mustafa.pdf")
                    .coverImageUrl(null)
                    .build();

            ImamResource r5 = ImamResource.builder()
                    .imamProfile(imam2Profile)
                    .type(ResourceType.BOOK)
                    .title("Bahar-e-Shariat (بہارِ شریعت جلد دوم - عبادات و روزمرہ مسائل)")
                    .description("[MASAIL] The universal encyclopedia of Hanafi Islamic rulings and daily jurisprudence • Author: Sadrush Shariah Allama Amjad Ali Azmi")
                    .fileUrl("https://example.com/books/bahar-e-shariat.pdf")
                    .coverImageUrl(null)
                    .build();

            imamResourceRepository.saveAll(List.of(r1, r2, r3, r4, r5));
        }

        // 7. Seed 24h Active Statuses
        if (imamStatusRepository.count() == 0) {
            System.out.println("Seeding active Imam statuses...");
            ImamStatus s1 = ImamStatus.builder()
                    .imamProfile(imam1Profile)
                    .contentText("Jumu'ah Mubarak! Today's Khutbah will be on the virtues of Tahajjud and steadfastness in prayer.")
                    .backgroundColorHex("#0A4D3C")
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            ImamStatus s2 = ImamStatus.builder()
                    .imamProfile(imam1Profile)
                    .contentText("Brothers and sisters: Note our Asr Jamaat will start at 4:45 PM starting this Saturday inshaaAllah.")
                    .backgroundColorHex("#8C6D23")
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            imamStatusRepository.saveAll(List.of(s1, s2));
        }

        // 8. Seed Masail Queries (Resolved FAQs & Pending Community Queue)
        if (masailQueryRepository.count() < 4) {
            System.out.println("Seeding community Masail queries and answered FAQs...");

            // Resolved FAQ 1
            MasailQuery q1 = MasailQuery.builder()
                    .user(regularUser1)
                    .imamProfile(imam1Profile)
                    .tag(QueryTag.MASAIL)
                    .userNameSnapshot(regularUser1.getName())
                    .userContactSnapshot(regularUser1.getPhone())
                    .localitySnapshot(regularUser1.getLocality())
                    .manhajSnapshot(regularUser1.getManhaj())
                    .questionText("Is it permissible to perform Wudu with bottled mineral water?")
                    .isUrgent(false)
                    .status(QueryStatus.RESOLVED)
                    .imamReplyText("Yes, it is completely permissible as long as the water is pure (Tahur) and not altered by impermissible impurities.")
                    .isPublicFaq(true)
                    .resolvedAt(LocalDateTime.now().minusDays(2))
                    .build();

            // Resolved FAQ 2
            MasailQuery q2 = MasailQuery.builder()
                    .user(regularUser2)
                    .imamProfile(imam1Profile)
                    .tag(QueryTag.MASAIL)
                    .userNameSnapshot(regularUser2.getName())
                    .userContactSnapshot(regularUser2.getPhone())
                    .localitySnapshot(regularUser2.getLocality())
                    .manhajSnapshot(regularUser2.getManhaj())
                    .questionText("Is the prayer valid if a person catches Ruku with the Imam before rising?")
                    .isUrgent(false)
                    .status(QueryStatus.RESOLVED)
                    .imamReplyText("Yes, according to the vast majority of jurists, catching the Ruku with the Imam counts as securing that entire Rak'ah.")
                    .isPublicFaq(true)
                    .resolvedAt(LocalDateTime.now().minusDays(1))
                    .build();

            // Pending Queue Question 1
            MasailQuery q3 = MasailQuery.builder()
                    .user(regularUser1)
                    .imamProfile(imam1Profile)
                    .tag(QueryTag.MASAIL)
                    .userNameSnapshot(regularUser1.getName())
                    .userContactSnapshot(regularUser1.getPhone())
                    .localitySnapshot(regularUser1.getLocality())
                    .manhajSnapshot(regularUser1.getManhaj())
                    .questionText("Can we combine Dhuhr and Asr prayers at the masjid during heavy storm and flood warnings?")
                    .isUrgent(false)
                    .status(QueryStatus.PENDING)
                    .isPublicFaq(false)
                    .build();

            // Pending Urgent Question 2
            MasailQuery q4 = MasailQuery.builder()
                    .user(regularUser2)
                    .imamProfile(imam1Profile)
                    .tag(QueryTag.MASAIL)
                    .userNameSnapshot(regularUser2.getName())
                    .userContactSnapshot(regularUser2.getPhone())
                    .localitySnapshot(regularUser2.getLocality())
                    .manhajSnapshot(regularUser2.getManhaj())
                    .questionText("Urgent Janazah inquiry: An elderly relative passed away abroad, can we perform Salatul Gha'ib (funeral in absentia)?")
                    .isUrgent(true)
                    .status(QueryStatus.PENDING)
                    .isPublicFaq(false)
                    .build();

            // Suggestion
            MasailQuery q5 = MasailQuery.builder()
                    .user(regularUser1)
                    .imamProfile(imam1Profile)
                    .tag(QueryTag.SUGGESTION)
                    .userNameSnapshot(regularUser1.getName())
                    .userContactSnapshot(regularUser1.getPhone())
                    .localitySnapshot(regularUser1.getLocality())
                    .manhajSnapshot(regularUser1.getManhaj())
                    .questionText("Could the Masjid administration organize a weekend tajweed workshop for children after Asr?")
                    .isUrgent(false)
                    .status(QueryStatus.PENDING)
                    .isPublicFaq(false)
                    .build();

            masailQueryRepository.saveAll(List.of(q1, q2, q3, q4, q5));
        }

        // 9. Seed Chat Consultation Requests & Conversation History
        if (chatRequestRepository.count() == 0) {
            System.out.println("Seeding chat consultation sessions and conversation history...");

            // Accepted Consultation with Message Exchange
            ChatRequest cr1 = ChatRequest.builder()
                    .user(regularUser1)
                    .imamProfile(imam1Profile)
                    .initialReason("Consultation regarding family inheritance distribution.")
                    .status(ChatRequestStatus.ACCEPTED)
                    .createdAt(LocalDateTime.now().minusHours(3))
                    .respondedAt(LocalDateTime.now().minusHours(2))
                    .build();
            cr1 = chatRequestRepository.save(cr1);

            ChatMessage m1 = ChatMessage.builder()
                    .chatRequest(cr1)
                    .sender(regularUser1)
                    .messageText("As-salamu alaykum Sheikh, I would like to consult with you regarding a personal family inheritance distribution.")
                    .sentAt(LocalDateTime.now().minusMinutes(50))
                    .readAt(LocalDateTime.now().minusMinutes(45))
                    .build();

            ChatMessage m2 = ChatMessage.builder()
                    .chatRequest(cr1)
                    .sender(imam1User)
                    .messageText("Wa alaykum as-salam wa rahmatullah. Please share the details of the heirs and estate, and we will calculate the Shariah shares.")
                    .sentAt(LocalDateTime.now().minusMinutes(40))
                    .readAt(LocalDateTime.now().minusMinutes(35))
                    .build();

            ChatMessage m3 = ChatMessage.builder()
                    .chatRequest(cr1)
                    .sender(regularUser1)
                    .messageText("JazakAllah khair Sheikh, the estate includes a residential property and two surviving sons and one daughter.")
                    .sentAt(LocalDateTime.now().minusMinutes(30))
                    .readAt(null)
                    .build();

            chatMessageRepository.saveAll(List.of(m1, m2, m3));

            // Pending Consultation Awaiting Imam Review
            ChatRequest cr2 = ChatRequest.builder()
                    .user(regularUser2)
                    .imamProfile(imam1Profile)
                    .initialReason("Requesting a 15-minute consultation regarding Nikah contract arrangements.")
                    .status(ChatRequestStatus.PENDING)
                    .createdAt(LocalDateTime.now().minusMinutes(15))
                    .build();
            chatRequestRepository.save(cr2);
        }

        // 9. Seed Home Banners (Max 4)
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

        System.out.println("✅ Angus Masail Database Seeding Complete!");
        System.out.println("👉 Default Imam User: 1234567890 (OTP: 123456) - Masjid Al-Noor");
        System.out.println("👉 Regular Muqtadi User: 0987654321 (OTP: 123456) - Tariq Mansoor");
    }
}
