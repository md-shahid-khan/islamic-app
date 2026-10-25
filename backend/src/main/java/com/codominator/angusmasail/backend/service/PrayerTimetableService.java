package com.codominator.angusmasail.backend.service;

import com.codominator.angusmasail.backend.dto.PrayerTimetableDto;
import com.codominator.angusmasail.backend.entity.ImamProfile;
import com.codominator.angusmasail.backend.entity.PrayerTimetable;
import com.codominator.angusmasail.backend.exception.ResourceNotFoundException;
import com.codominator.angusmasail.backend.repository.ImamProfileRepository;
import com.codominator.angusmasail.backend.repository.PrayerTimetableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PrayerTimetableService {

    @Autowired
    private PrayerTimetableRepository prayerTimetableRepository;

    @Autowired
    private ImamProfileRepository imamProfileRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PRAYER_CACHE_KEY = "prayer:";

    public PrayerTimetableDto getDailyTimetable(UUID imamProfileId, LocalDate date) {
        String cacheKey = PRAYER_CACHE_KEY + imamProfileId + ":" + date;
        
        // Return if in Redis cache
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof PrayerTimetableDto) {
                return (PrayerTimetableDto) cached;
            }
        } catch (Exception e) {
            System.err.println("Redis cache read skipped: " + e.getMessage());
        }

        ImamProfile imamProfile = imamProfileRepository.findById(imamProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Imam profile not found"));

        Optional<PrayerTimetable> timetableOpt = prayerTimetableRepository.findByImamProfileIdAndDate(imamProfileId, date);
        
        PrayerTimetable timetable;
        if (timetableOpt.isPresent()) {
            timetable = timetableOpt.get();
        } else {
            // Fallback to latest saved row
            timetable = prayerTimetableRepository.findFirstByImamProfileIdOrderByDateDesc(imamProfileId)
                    .orElseGet(() -> PrayerTimetable.builder()
                            .imamProfile(imamProfile)
                            .date(date)
                            .build());
        }

        PrayerTimetableDto dto = mapToDto(timetable, imamProfile.getHijriOffsetDays());
        try {
            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofHours(12));
        } catch (Exception e) {
            System.err.println("Redis cache write skipped: " + e.getMessage());
        }
        return dto;
    }

    @Transactional
    public PrayerTimetableDto updateTimetable(UUID userId, PrayerTimetableDto requestDto) {
        ImamProfile imamProfile = null;
        if (requestDto.getImamProfileId() != null) {
            imamProfile = imamProfileRepository.findById(requestDto.getImamProfileId()).orElse(null);
        }
        if (imamProfile == null && userId != null) {
            imamProfile = imamProfileRepository.findByUserId(userId).orElse(null);
        }
        if (imamProfile == null) {
            imamProfile = imamProfileRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No Imam profile found in database"));
        }

        if (requestDto.getHijriOffsetDays() != null) {
            imamProfile.setHijriOffsetDays(requestDto.getHijriOffsetDays());
            imamProfileRepository.save(imamProfile);
        }

        LocalDate date = requestDto.getDate() != null ? requestDto.getDate() : LocalDate.now();
        Optional<PrayerTimetable> existingOpt = prayerTimetableRepository.findByImamProfileIdAndDate(imamProfile.getId(), date);

        final ImamProfile targetImam = imamProfile;
        PrayerTimetable timetable = existingOpt.orElseGet(() -> PrayerTimetable.builder()
                .imamProfile(targetImam)
                .date(date)
                .build());

        // Authoritative start and end window times (from request, existing timetable, or official Sunnah/fiqh defaults)
        LocalTime fStart = requestDto.getFajrStartTime() != null ? requestDto.getFajrStartTime() :
                (timetable.getFajrStartTime() != null ? timetable.getFajrStartTime() : LocalTime.of(4, 30));
        LocalTime fEnd = requestDto.getFajrEndTime() != null ? requestDto.getFajrEndTime() :
                (timetable.getFajrEndTime() != null ? timetable.getFajrEndTime() : (timetable.getSunrise() != null ? timetable.getSunrise() : LocalTime.of(6, 15)));

        LocalTime sStart = requestDto.getSunriseStartTime() != null ? requestDto.getSunriseStartTime() :
                (timetable.getSunriseStartTime() != null ? timetable.getSunriseStartTime() : fEnd);
        LocalTime sEnd = requestDto.getSunriseEndTime() != null ? requestDto.getSunriseEndTime() :
                (timetable.getSunriseEndTime() != null ? timetable.getSunriseEndTime() : LocalTime.of(12, 30));

        LocalTime dStart = requestDto.getDhuhrStartTime() != null ? requestDto.getDhuhrStartTime() :
                (timetable.getDhuhrStartTime() != null ? timetable.getDhuhrStartTime() : LocalTime.of(12, 30));
        LocalTime dEnd = requestDto.getDhuhrEndTime() != null ? requestDto.getDhuhrEndTime() :
                (timetable.getDhuhrEndTime() != null ? timetable.getDhuhrEndTime() : LocalTime.of(16, 15));

        LocalTime aStart = requestDto.getAsrStartTime() != null ? requestDto.getAsrStartTime() :
                (timetable.getAsrStartTime() != null ? timetable.getAsrStartTime() : LocalTime.of(16, 15));
        LocalTime aEnd = requestDto.getAsrEndTime() != null ? requestDto.getAsrEndTime() :
                (timetable.getAsrEndTime() != null ? timetable.getAsrEndTime() : LocalTime.of(18, 45));

        LocalTime mStart = requestDto.getMaghribStartTime() != null ? requestDto.getMaghribStartTime() :
                (timetable.getMaghribStartTime() != null ? timetable.getMaghribStartTime() : LocalTime.of(18, 45));
        LocalTime mEnd = requestDto.getMaghribEndTime() != null ? requestDto.getMaghribEndTime() :
                (timetable.getMaghribEndTime() != null ? timetable.getMaghribEndTime() : LocalTime.of(20, 0));

        LocalTime iStart = requestDto.getIshaStartTime() != null ? requestDto.getIshaStartTime() :
                (timetable.getIshaStartTime() != null ? timetable.getIshaStartTime() : LocalTime.of(20, 0));
        LocalTime iEnd = requestDto.getIshaEndTime() != null ? requestDto.getIshaEndTime() :
                (timetable.getIshaEndTime() != null ? timetable.getIshaEndTime() : fStart);

        LocalTime jStart = requestDto.getJumuahStartTime() != null ? requestDto.getJumuahStartTime() :
                (timetable.getJumuahStartTime() != null ? timetable.getJumuahStartTime() : dStart);
        LocalTime jEnd = requestDto.getJumuahEndTime() != null ? requestDto.getJumuahEndTime() :
                (timetable.getJumuahEndTime() != null ? timetable.getJumuahEndTime() : dEnd);

        // Validation: Imam's entered congregational prayer times MUST come within the start & end window!
        validatePrayerTime("Fajr", requestDto.getFajrAzan(), "Azan", fStart, fEnd);
        validatePrayerTime("Fajr", requestDto.getFajrJamaat(), "Jamaat", fStart, fEnd);
        validateOrder("Fajr", requestDto.getFajrAzan(), requestDto.getFajrJamaat());

        validatePrayerTime("Dhuhr", requestDto.getDhuhrAzan(), "Azan", dStart, dEnd);
        validatePrayerTime("Dhuhr", requestDto.getDhuhrJamaat(), "Jamaat", dStart, dEnd);
        validateOrder("Dhuhr", requestDto.getDhuhrAzan(), requestDto.getDhuhrJamaat());

        validatePrayerTime("Asr", requestDto.getAsrAzan(), "Azan", aStart, aEnd);
        validatePrayerTime("Asr", requestDto.getAsrJamaat(), "Jamaat", aStart, aEnd);
        validateOrder("Asr", requestDto.getAsrAzan(), requestDto.getAsrJamaat());

        validatePrayerTime("Maghrib", requestDto.getMaghribAzan(), "Azan", mStart, mEnd);
        validatePrayerTime("Maghrib", requestDto.getMaghribJamaat(), "Jamaat", mStart, mEnd);
        validateOrder("Maghrib", requestDto.getMaghribAzan(), requestDto.getMaghribJamaat());

        validatePrayerTime("Isha", requestDto.getIshaAzan(), "Azan", iStart, iEnd);
        validatePrayerTime("Isha", requestDto.getIshaJamaat(), "Jamaat", iStart, iEnd);
        if (requestDto.getIshaAzan() != null && requestDto.getIshaJamaat() != null) {
            LocalTime az = requestDto.getIshaAzan();
            LocalTime jm = requestDto.getIshaJamaat();
            if ((az.isAfter(LocalTime.NOON) && jm.isAfter(LocalTime.NOON)) || (az.isBefore(LocalTime.NOON) && jm.isBefore(LocalTime.NOON))) {
                validateOrder("Isha", az, jm);
            }
        }

        validatePrayerTime("Jumu'ah", requestDto.getJumuahAzan(), "Azan", jStart, jEnd);
        validatePrayerTime("Jumu'ah", requestDto.getJumuahJamaat(), "Jamaat", jStart, jEnd);
        validateOrder("Jumu'ah", requestDto.getJumuahAzan(), requestDto.getJumuahJamaat());

        // Update Start & End windows if provided, else keep authoritative
        timetable.setFajrStartTime(fStart);
        timetable.setFajrEndTime(fEnd);
        timetable.setSunriseStartTime(sStart);
        timetable.setSunriseEndTime(sEnd);
        timetable.setDhuhrStartTime(dStart);
        timetable.setDhuhrEndTime(dEnd);
        timetable.setAsrStartTime(aStart);
        timetable.setAsrEndTime(aEnd);
        timetable.setMaghribStartTime(mStart);
        timetable.setMaghribEndTime(mEnd);
        timetable.setIshaStartTime(iStart);
        timetable.setIshaEndTime(iEnd);
        timetable.setJumuahStartTime(jStart);
        timetable.setJumuahEndTime(jEnd);

        // Update Azan and Jamaat
        if (requestDto.getFajrAzan() != null) timetable.setFajrAzan(requestDto.getFajrAzan());
        if (requestDto.getFajrJamaat() != null) timetable.setFajrJamaat(requestDto.getFajrJamaat());
        if (requestDto.getSunrise() != null) timetable.setSunrise(requestDto.getSunrise());
        if (requestDto.getDhuhrAzan() != null) timetable.setDhuhrAzan(requestDto.getDhuhrAzan());
        if (requestDto.getDhuhrJamaat() != null) timetable.setDhuhrJamaat(requestDto.getDhuhrJamaat());
        if (requestDto.getAsrAzan() != null) timetable.setAsrAzan(requestDto.getAsrAzan());
        if (requestDto.getAsrJamaat() != null) timetable.setAsrJamaat(requestDto.getAsrJamaat());
        if (requestDto.getMaghribAzan() != null) timetable.setMaghribAzan(requestDto.getMaghribAzan());
        if (requestDto.getMaghribJamaat() != null) timetable.setMaghribJamaat(requestDto.getMaghribJamaat());
        if (requestDto.getIshaAzan() != null) timetable.setIshaAzan(requestDto.getIshaAzan());
        if (requestDto.getIshaJamaat() != null) timetable.setIshaJamaat(requestDto.getIshaJamaat());
        if (requestDto.getJumuahAzan() != null) timetable.setJumuahAzan(requestDto.getJumuahAzan());
        if (requestDto.getJumuahJamaat() != null) timetable.setJumuahJamaat(requestDto.getJumuahJamaat());

        timetable = prayerTimetableRepository.save(timetable);

        // Evict cache
        try {
            String cacheKey = PRAYER_CACHE_KEY + imamProfile.getId() + ":" + date;
            redisTemplate.delete(cacheKey);
        } catch (Exception ignored) {}

        // Broadcast FCM notification asynchronously
        try {
            notificationService.notifyImamFollowers(
                    imamProfile.getId(),
                    "Prayer Time Updated",
                    "Prayer times for " + date + " at " + imamProfile.getMasjidName() + " have been updated."
            );
        } catch (Exception ignored) {}

        return mapToDto(timetable, imamProfile.getHijriOffsetDays());
    }

    private void validatePrayerTime(String prayerName, LocalTime time, String type, LocalTime start, LocalTime end) {
        if (time == null) return;
        boolean valid;
        if (end.isAfter(start)) {
            valid = !time.isBefore(start) && !time.isAfter(end);
        } else {
            // Overnight window (e.g., Isha 20:00 to 04:30)
            valid = !time.isBefore(start) || !time.isAfter(end);
        }
        if (!valid) {
            throw new IllegalArgumentException(prayerName + " " + type + " (" + time + ") must fall within the prayer window between " + start + " and " + end + ".");
        }
    }

    private void validateOrder(String prayerName, LocalTime azan, LocalTime jamaat) {
        if (azan != null && jamaat != null) {
            if (jamaat.isBefore(azan)) {
                throw new IllegalArgumentException(prayerName + " Jamaat time (" + jamaat + ") cannot be earlier than Azan time (" + azan + ").");
            }
        }
    }

    private PrayerTimetableDto mapToDto(PrayerTimetable timetable, Integer hijriOffsetDays) {
        LocalTime fStart = timetable.getFajrStartTime() != null ? timetable.getFajrStartTime() : LocalTime.of(4, 30);
        LocalTime fEnd = timetable.getFajrEndTime() != null ? timetable.getFajrEndTime() :
                (timetable.getSunrise() != null ? timetable.getSunrise() : LocalTime.of(6, 15));

        LocalTime sStart = timetable.getSunriseStartTime() != null ? timetable.getSunriseStartTime() : fEnd;
        LocalTime sEnd = timetable.getSunriseEndTime() != null ? timetable.getSunriseEndTime() : LocalTime.of(12, 30);

        LocalTime dStart = timetable.getDhuhrStartTime() != null ? timetable.getDhuhrStartTime() : LocalTime.of(12, 30);
        LocalTime dEnd = timetable.getDhuhrEndTime() != null ? timetable.getDhuhrEndTime() : LocalTime.of(16, 15);

        LocalTime aStart = timetable.getAsrStartTime() != null ? timetable.getAsrStartTime() : LocalTime.of(16, 15);
        LocalTime aEnd = timetable.getAsrEndTime() != null ? timetable.getAsrEndTime() : LocalTime.of(18, 45);

        LocalTime mStart = timetable.getMaghribStartTime() != null ? timetable.getMaghribStartTime() : LocalTime.of(18, 45);
        LocalTime mEnd = timetable.getMaghribEndTime() != null ? timetable.getMaghribEndTime() : LocalTime.of(20, 0);

        LocalTime iStart = timetable.getIshaStartTime() != null ? timetable.getIshaStartTime() : LocalTime.of(20, 0);
        LocalTime iEnd = timetable.getIshaEndTime() != null ? timetable.getIshaEndTime() : fStart;

        LocalTime jStart = timetable.getJumuahStartTime() != null ? timetable.getJumuahStartTime() : dStart;
        LocalTime jEnd = timetable.getJumuahEndTime() != null ? timetable.getJumuahEndTime() : dEnd;

        return PrayerTimetableDto.builder()
                .id(timetable.getId())
                .imamProfileId(timetable.getImamProfile().getId())
                .date(timetable.getDate())
                .fajrStartTime(fStart)
                .fajrEndTime(fEnd)
                .fajrAzan(timetable.getFajrAzan())
                .fajrJamaat(timetable.getFajrJamaat())
                .sunriseStartTime(sStart)
                .sunriseEndTime(sEnd)
                .sunrise(timetable.getSunrise() != null ? timetable.getSunrise() : sStart)
                .dhuhrStartTime(dStart)
                .dhuhrEndTime(dEnd)
                .dhuhrAzan(timetable.getDhuhrAzan())
                .dhuhrJamaat(timetable.getDhuhrJamaat())
                .asrStartTime(aStart)
                .asrEndTime(aEnd)
                .asrAzan(timetable.getAsrAzan())
                .asrJamaat(timetable.getAsrJamaat())
                .maghribStartTime(mStart)
                .maghribEndTime(mEnd)
                .maghribAzan(timetable.getMaghribAzan())
                .maghribJamaat(timetable.getMaghribJamaat())
                .ishaStartTime(iStart)
                .ishaEndTime(iEnd)
                .ishaAzan(timetable.getIshaAzan())
                .ishaJamaat(timetable.getIshaJamaat())
                .jumuahStartTime(jStart)
                .jumuahEndTime(jEnd)
                .jumuahAzan(timetable.getJumuahAzan())
                .jumuahJamaat(timetable.getJumuahJamaat())
                .hijriOffsetDays(hijriOffsetDays != null ? hijriOffsetDays : 0)
                .build();
    }
}
