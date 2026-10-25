package com.codominator.angusmasail.backend.repository;

import com.codominator.angusmasail.backend.entity.PrayerTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrayerTimetableRepository extends JpaRepository<PrayerTimetable, UUID> {
    Optional<PrayerTimetable> findByImamProfileIdAndDate(UUID imamProfileId, LocalDate date);
    Optional<PrayerTimetable> findFirstByImamProfileIdOrderByDateDesc(UUID imamProfileId);
    List<PrayerTimetable> findByImamProfileId(UUID imamProfileId);

    @Modifying
    @Query("DELETE FROM PrayerTimetable pt WHERE pt.imamProfile.id = :imamProfileId")
    int deleteByImamProfileId(@Param("imamProfileId") UUID imamProfileId);
}
