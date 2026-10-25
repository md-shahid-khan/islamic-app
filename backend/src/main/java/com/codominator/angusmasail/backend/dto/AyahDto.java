package com.codominator.angusmasail.backend.dto;

public class AyahDto {
    private int numberInSurah;
    private int globalNumber;
    private String arabicText;
    private String urduTranslation; // Strictly Kanzul Iman by Ala Hazrat Ahmad Raza Khan Barelvi
    private String audioUrl;
    private Integer juz;
    private Integer page;
    private Integer surahNumber;
    private String surahName;

    public AyahDto() {}

    public AyahDto(int numberInSurah, int globalNumber, String arabicText, String urduTranslation, String audioUrl, Integer juz, Integer page) {
        this.numberInSurah = numberInSurah;
        this.globalNumber = globalNumber;
        this.arabicText = arabicText;
        this.urduTranslation = urduTranslation;
        this.audioUrl = audioUrl;
        this.juz = juz;
        this.page = page;
    }

    public AyahDto(int numberInSurah, int globalNumber, String arabicText, String urduTranslation, String audioUrl, Integer juz, Integer page, Integer surahNumber, String surahName) {
        this.numberInSurah = numberInSurah;
        this.globalNumber = globalNumber;
        this.arabicText = arabicText;
        this.urduTranslation = urduTranslation;
        this.audioUrl = audioUrl;
        this.juz = juz;
        this.page = page;
        this.surahNumber = surahNumber;
        this.surahName = surahName;
    }

    public Integer getSurahNumber() { return surahNumber; }
    public void setSurahNumber(Integer surahNumber) { this.surahNumber = surahNumber; }

    public String getSurahName() { return surahName; }
    public void setSurahName(String surahName) { this.surahName = surahName; }

    public int getNumberInSurah() { return numberInSurah; }
    public void setNumberInSurah(int numberInSurah) { this.numberInSurah = numberInSurah; }

    public int getGlobalNumber() { return globalNumber; }
    public void setGlobalNumber(int globalNumber) { this.globalNumber = globalNumber; }

    public String getArabicText() { return arabicText; }
    public void setArabicText(String arabicText) { this.arabicText = arabicText; }

    public String getUrduTranslation() { return urduTranslation; }
    public void setUrduTranslation(String urduTranslation) { this.urduTranslation = urduTranslation; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public Integer getJuz() { return juz; }
    public void setJuz(Integer juz) { this.juz = juz; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
}
