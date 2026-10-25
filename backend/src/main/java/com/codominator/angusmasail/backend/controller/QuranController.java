package com.codominator.angusmasail.backend.controller;

import com.codominator.angusmasail.backend.dto.AyahDto;
import com.codominator.angusmasail.backend.dto.SurahDetailDto;
import com.codominator.angusmasail.backend.dto.SurahHeaderDto;
import com.codominator.angusmasail.backend.service.QuranService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quran")
@CrossOrigin(origins = "*")
public class QuranController {

    private final QuranService quranService;

    public QuranController(QuranService quranService) {
        this.quranService = quranService;
    }

    @GetMapping("/surahs")
    public ResponseEntity<List<SurahHeaderDto>> getAllSurahs() {
        return ResponseEntity.ok(quranService.getAllSurahs());
    }

    @GetMapping("/surah/{surahNumber}")
    public ResponseEntity<SurahDetailDto> getSurah(@PathVariable("surahNumber") int surahNumber) {
        return ResponseEntity.ok(quranService.getSurah(surahNumber));
    }

    @GetMapping("/ayah/{surahNumber}/{ayahNumber}")
    public ResponseEntity<AyahDto> getAyah(
            @PathVariable("surahNumber") int surahNumber,
            @PathVariable("ayahNumber") int ayahNumber
    ) {
        return ResponseEntity.ok(quranService.getAyah(surahNumber, ayahNumber));
    }

    @GetMapping("/ayah-of-the-day")
    public ResponseEntity<AyahDto> getAyahOfTheDay() {
        return ResponseEntity.ok(quranService.getAyahOfTheDay());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SurahHeaderDto>> searchSurahs(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(quranService.getAllSurahs());
        }
        String clean = query.trim().toLowerCase();
        List<SurahHeaderDto> matched = new ArrayList<>();
        for (SurahHeaderDto s : quranService.getAllSurahs()) {
            if (String.valueOf(s.getNumber()).equals(clean) ||
                s.getEnglishName().toLowerCase().contains(clean) ||
                s.getName().contains(query) ||
                s.getEnglishNameTranslation().toLowerCase().contains(clean)) {
                matched.add(s);
            }
        }
        return ResponseEntity.ok(matched);
    }
}
