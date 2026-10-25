package com.codominator.angusmasail.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrayerTimetableDto {
    private UUID id;
    private UUID imamProfileId;
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

    private Integer hijriOffsetDays;
}
