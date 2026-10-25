package com.codominator.angusmasail.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "prayer_timetables", uniqueConstraints = {
    @UniqueConstraint(name = "uk_imam_date", columnNames = {"imam_profile_id", "date"})
}, indexes = {
    @Index(name = "idx_timetable_imam_date", columnList = "imam_profile_id, date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrayerTimetable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imam_profile_id", nullable = false)
    private ImamProfile imamProfile;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime fajrStartTime;
    private LocalTime fajrEndTime;
    private LocalTime fajrAzan;
    private LocalTime fajrJamaat;

    private LocalTime sunriseStartTime;
    private LocalTime sunriseEndTime;
    private LocalTime sunrise;

    private LocalTime dhuhrStartTime;
    private LocalTime dhuhrEndTime;
    private LocalTime dhuhrAzan;
    private LocalTime dhuhrJamaat;

    private LocalTime asrStartTime;
    private LocalTime asrEndTime;
    private LocalTime asrAzan;
    private LocalTime asrJamaat;

    private LocalTime maghribStartTime;
    private LocalTime maghribEndTime;
    private LocalTime maghribAzan;
    private LocalTime maghribJamaat;

    private LocalTime ishaStartTime;
    private LocalTime ishaEndTime;
    private LocalTime ishaAzan;
    private LocalTime ishaJamaat;

    private LocalTime jumuahStartTime;
    private LocalTime jumuahEndTime;
    private LocalTime jumuahAzan;
    private LocalTime jumuahJamaat;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
