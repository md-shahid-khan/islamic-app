package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.config.UserDetailsImpl;
import com.codominator.angusmasail.backend.dto.PrayerTimetableDto;
import com.codominator.angusmasail.backend.service.PrayerTimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PrayerTimetableController {

    @Autowired
    private PrayerTimetableService prayerTimetableService;

    @GetMapping("/prayer/daily")
    public ResponseEntity<PrayerTimetableDto> getDailyTimetable(
            @RequestParam UUID imamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(prayerTimetableService.getDailyTimetable(imamId, queryDate));
    }

    @PutMapping("/imam/prayer/update")
    public ResponseEntity<PrayerTimetableDto> updateTimetable(
            @RequestBody PrayerTimetableDto requestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UUID userId = userDetails != null ? userDetails.getId() : null;
        return ResponseEntity.ok(prayerTimetableService.updateTimetable(userId, requestDto));
    }
}
